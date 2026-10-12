package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.BuildConfig
import io.github.alexistrejo.pimienta.pos.app.deviceSession

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.domain.Money
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CatalogCategoryEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import java.math.BigDecimal
import java.util.UUID

const val PRODUCTION_API_URL = "https://api.pimienta-alimentos.com"

class ProvisioningRepository(private val context: Context, private val provider: PosDatabaseProvider) {
    private val db get() = provider.database(RuntimeMode.PRODUCTION)
    // All HTTP + token handling is delegated to DeviceSession. The repository keeps the catalog,
    // enrollment, and bootstrap/delta responsibilities; it never builds a Retrofit client itself.
    private val session = context.deviceSession()
    private val json = DeviceApiFactory.json()
    private val prefs = context.getSharedPreferences("pos-device-config", Context.MODE_PRIVATE)

    fun baseUrl(): String? = db.syncDao().state()?.baseUrl

    fun needsEnrollment(): Boolean {
        val state = db.syncDao().state()
        return state?.baseUrl.isNullOrBlank() || state?.status == "REQUIRES_REENROLLMENT"
    }

    /**
     * Clears local session so the tablet can enroll again after revoke / dead refresh token.
     * Keeps the stable device publicId so the server can re-authorize the same tablet.
     */
    fun resetForReenrollment(reason: String) {
        session.clearTokens()
        db.runInTransaction {
            db.productDao().clear()
            db.userDao().clear()
            db.siteDao().clear()
            db.syncDao().saveState(
                (db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(
                    baseUrl = null,
                    changesCursor = null,
                    bootstrapSnapshotId = null,
                    lastSuccessfulAtEpochMillis = null,
                    lastError = reason,
                    status = "REQUIRES_REENROLLMENT",
                ),
            )
        }
        SyncWorker.cancel(context)
    }

    fun configureUrl(url: String) {
        requireAcceptableApiUrl(url)
        db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(baseUrl = url, status = "CONFIGURED"))
    }

    // Rejects cleartext production URLs before any network call or local write.
    private fun requireAcceptableApiUrl(url: String) {
        require(url.startsWith("https://") || (BuildConfig.DEBUG && (url.startsWith("http://10.0.2.2") || url.startsWith("http://localhost")))) {
            "La URL debe usar HTTPS en release"
        }
    }

    suspend fun enroll(baseUrl: String, code: String, name: String): EnrollResponse {
        // Do not publish the URL until the server accepts the code. An early write lets reload
        // see a base URL with no tokens and wipe the session that this call is about to save.
        requireAcceptableApiUrl(baseUrl)
        val publicId = prefs.getString("publicId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("publicId", it).apply() }
        val api = session.unauthenticatedApi(baseUrl)
        val result = api.enroll(EnrollRequest(code, publicId, name, BuildConfig.VERSION_NAME))
        session.saveTokens(result.accessToken, result.refreshToken)
        // Same tablet keeps its causal clock: never restart at 1 after revoke/re-enroll.
        val nextSequence = DeviceSequenceClock.next(
            localNext = db.operationsDao().device()?.nextEventSequence,
            serverLast = result.lastDeviceSequence,
        )
        val device = DeviceEntity(
            result.deviceId,
            name,
            result.visibleCode,
            nextSequence,
            result.site.id,
            result.status,
            result.minAppVersion,
            json.encodeToString(result.eventSchemaVersions),
        )
        db.runInTransaction {
            db.siteDao().clear()
            db.siteDao().insert(SiteEntity(result.site.id, result.site.name, result.site.address, result.site.currency))
            db.operationsDao().insertDevice(device)
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(baseUrl = baseUrl, status = "ENROLLED", lastError = null))
        }
        // Catalog import can fail independently; tokens/device are already persisted for SyncWorker retry.
        try {
            applyBootstrap(session.authenticatedApi(baseUrl).bootstrap())
        } catch (e: Exception) {
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(baseUrl = baseUrl, status = "RETRYING", lastError = e.message))
            throw e
        }
        return result
    }

    /**
     * Creates a POS-sellable product on the server and upserts it into Room.
     * Does not invent a local id when the call fails.
     */
    suspend fun createProduct(
        name: String,
        salePriceCentavos: Long,
        saleCategory: String,
        barcode: String?,
        createdByOperatorId: Long?,
        controlledStock: Boolean,
    ): Result<ProductEntity> {
        val request = CreatePosProductRequest(
            name = name.trim(),
            salePriceCentavos = salePriceCentavos,
            saleCategory = saleCategory.trim(),
            barcode = barcode?.trim()?.takeIf { it.isNotEmpty() },
            createdByOperatorId = createdByOperatorId,
            stockPolicy = if (controlledStock) "CONTROLLED" else "NOT_CONTROLLED",
        )
        return callDevice { api -> persistCreatedProduct(api.createProduct(request)) }
    }

    /**
     * Updates name and/or the site offer. Skips the call for a side that did not change
     * and writes Room after each successful response.
     */
    suspend fun updateProduct(
        product: ProductEntity,
        name: String,
        salePriceCentavos: Long,
        controlledStock: Boolean,
        barcode: String?,
        saleCategory: String,
    ): Result<ProductEntity> {
        val plan = planProductEdit(
            originalName = product.name,
            originalPriceCentavos = Money.fromCatalog(product.price),
            originalControlled = product.stockPolicy == "CONTROLLED",
            originalBarcode = product.barcode,
            sku = product.sku,
            name = name,
            priceCentavos = salePriceCentavos,
            controlled = controlledStock,
            barcode = barcode,
            originalCategory = product.saleCategory,
            category = saleCategory,
        )
        if (!plan.rename && !plan.offer) return Result.success(product)
        var saved = product
        return try {
            applyPlannedProductEdit(
                plan = plan,
                rename = {
                    callDevice { api ->
                        api.renameProduct(product.id, RenamePosProductRequest(name.trim(), catalogBarcode(barcode, product.sku)))
                    }.getOrThrow()
                },
                offer = {
                    callDevice { api ->
                        api.updateProductOffer(
                            product.id,
                            UpdatePosProductOfferRequest(
                                salePriceCentavos,
                                if (controlledStock) "CONTROLLED" else "NOT_CONTROLLED",
                                saleCategory.trim(),
                            ),
                        )
                    }.getOrThrow()
                },
                persist = { dto -> saved = persistCreatedProduct(dto) },
            )
            Result.success(saved)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    // One device call, refreshing the access token once on 401.
    // Error messages preserved verbatim so existing UI copy and tests do not shift.
    // A refresh failure here does NOT trigger a reenrollment reset; only the sync pipeline wipes the session.
    private suspend fun <T> callDevice(block: suspend (DeviceApi) -> T): Result<T> {
        val state = db.syncDao().state()
        val baseUrl = state?.baseUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Result.failure(IllegalStateException("Dispositivo no enrolado."))
        return when (val outcome = session.withAuth(baseUrl) { api -> block(api) }) {
            is AuthenticatedCallOutcome.Success -> Result.success(outcome.value)
            AuthenticatedCallOutcome.MissingAccess ->
                Result.failure(IllegalStateException("Sesión del dispositivo inválida. Vuelve a enrolar."))
            AuthenticatedCallOutcome.MissingRefresh ->
                Result.failure(IllegalStateException(DeviceSessionPolicy.missingRefreshTokenMessage()))
            is AuthenticatedCallOutcome.CallFailed -> Result.failure(outcome.error)
            is AuthenticatedCallOutcome.RefreshFailed -> Result.failure(outcome.error)
        }
    }

    private fun persistCreatedProduct(dto: ProductDto): ProductEntity {
        val product = dto.toProductEntity()
        db.runInTransaction {
            db.productDao().insertAll(listOf(product))
            val siteId = db.siteDao().current()?.id
            if (siteId != null && product.saleCategory.isNotBlank()) {
                db.syncProjectionDao().insertCategories(listOf(CatalogCategoryEntity(siteId, product.saleCategory)))
            }
        }
        return product
    }

    // Applies catalog/operator delta operations atomically and advances the cursor after commit.
    fun applyChanges(changes: ChangesResponse, syncedEvents: List<EventResult> = emptyList()) {
        db.runInTransaction {
            val siteId = db.siteDao().current()?.id
                ?: throw IllegalStateException("Cannot apply POS changes without a local site")
            changes.operations.forEach { op ->
                when (op.entity.lowercase()) {
                    "product" -> when (op.op.lowercase()) {
                        "deactivate" -> db.productDao().deleteById(op.id)
                        "upsert" -> {
                            val product = op.data?.let { json.decodeFromJsonElement(ProductDto.serializer(), it).toProductEntity() }
                                ?: throw IllegalArgumentException("Product upsert has no payload")
                            db.productDao().insertAll(listOf(product))
                            db.syncProjectionDao().insertCategories(
                                listOf(CatalogCategoryEntity(siteId, product.saleCategory)),
                            )
                        }
                        else -> throw IllegalArgumentException("Unsupported product operation: ${op.op}")
                    }
                    "operator", "user" -> when (op.op.lowercase()) {
                        "deactivate" -> db.userDao().deleteById(op.id)
                        "upsert" -> {
                            val operator = op.data?.let { json.decodeFromJsonElement(OperatorDto.serializer(), it).toUser() }
                                ?: throw IllegalArgumentException("Operator upsert has no payload")
                            db.userDao().insertAll(listOf(operator))
                        }
                        else -> throw IllegalArgumentException("Unsupported operator operation: ${op.op}")
                    }
                    "policies", "policy" -> when (op.op.lowercase()) {
                        "upsert" -> {
                            val policy = op.data?.let {
                                json.decodeFromJsonElement(PoliciesDto.serializer(), it)
                            } ?: throw IllegalArgumentException("Policy upsert has no payload")
                            db.syncProjectionDao().insertPolicy(policy.toEntity(siteId))
                            db.syncProjectionDao().insertCategories(
                                policy.openAmountCategories.filter(String::isNotBlank).distinct()
                                    .map { CatalogCategoryEntity(siteId, it) },
                            )
                        }
                        "deactivate" -> {
                            db.syncProjectionDao().clearPolicy()
                            db.syncProjectionDao().clearCategories()
                        }
                        else -> throw IllegalArgumentException("Unsupported policy operation: ${op.op}")
                    }
                    "site" -> when (op.op.lowercase()) {
                        "deactivate" -> db.siteDao().deleteById(op.id)
                        "upsert" -> {
                            val site = op.data?.let { json.decodeFromJsonElement(SiteDto.serializer(), it).toSite() }
                                ?: throw IllegalArgumentException("Site upsert has no payload")
                            db.siteDao().insert(site)
                        }
                        else -> throw IllegalArgumentException("Unsupported site operation: ${op.op}")
                    }
                    else -> throw IllegalArgumentException("Unsupported POS change entity: ${op.entity}")
                }
            }
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(changesCursor = changes.nextCursor, lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
            syncedEvents.forEach { db.syncDao().markSynced(it.eventId, it.status, it.incidentId, it.message) }
        }
    }
    private fun PoliciesDto.toEntity(siteId: String) = PosPolicyEntity(
        siteId = siteId,
        allowNegativeStock = allowNegativeStock,
        allowOpenProducts = allowOpenProducts,
        defaultNegativeStockLimit = defaultNegativeStockLimit,
        staleCatalogWarnHours = staleCatalogWarnHours,
        staleCatalogBlockHours = staleCatalogBlockHours,
        openAmountCategoriesJson = json.encodeToString(openAmountCategories.filter(String::isNotBlank).distinct()),
        stockless = stockless,
    )

    fun applyBootstrap(snapshot: BootstrapResponse, syncedEvents: List<EventResult> = emptyList()) {
        db.runInTransaction {
            db.siteDao().clear()
            db.productDao().clear()
            db.userDao().clear()
            db.syncProjectionDao().clearCategories()
            db.syncProjectionDao().clearPolicy()
            db.siteDao().insert(SiteEntity(snapshot.site.id, snapshot.site.name, snapshot.site.address, snapshot.site.currency))
            // Reconcile the causal clock with the server without wiping operational outbox rows.
            db.operationsDao().device()?.let { existing ->
                val nextSequence = DeviceSequenceClock.next(existing.nextEventSequence, snapshot.device.lastDeviceSequence)
                db.operationsDao().insertDevice(
                    existing.copy(
                        id = snapshot.device.id,
                        name = snapshot.device.name,
                        visibleCode = snapshot.device.visibleCode,
                        nextEventSequence = nextSequence,
                        siteId = snapshot.site.id,
                        status = snapshot.device.status,
                    ),
                )
            }
            db.productDao().insertAll(snapshot.products.map { it.toProductEntity() })
            db.userDao().insertAll(snapshot.operators.map { it.toUser() })
            val categories = (snapshot.products.map { it.saleCategory } + snapshot.openAmountCategories)
                .filter(String::isNotBlank)
                .distinct()
                .map { CatalogCategoryEntity(snapshot.site.id, it) }
            db.syncProjectionDao().insertCategories(categories)
            db.syncProjectionDao().insertPolicy(
                snapshot.policies.toEntity(snapshot.site.id).copy(
                    openAmountCategoriesJson = json.encodeToString(
                        snapshot.openAmountCategories.filter(String::isNotBlank).distinct(),
                    ),
                ),
            )
            db.bootstrapDao().insert(BootstrapEntity(snapshot.snapshotId, snapshot.schemaVersion, System.currentTimeMillis()))
            db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(changesCursor = snapshot.cursors.changes, bootstrapSnapshotId = snapshot.snapshotId, lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
            syncedEvents.forEach { db.syncDao().markSynced(it.eventId, it.status, it.incidentId, it.message) }
        }
    }

}
