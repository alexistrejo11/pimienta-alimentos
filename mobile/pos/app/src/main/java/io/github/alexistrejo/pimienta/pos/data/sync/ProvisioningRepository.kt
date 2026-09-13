package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.BuildConfig

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import java.math.BigDecimal
import java.util.UUID
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException

const val PRODUCTION_API_URL = "https://api.pimienta-alimentos.com"

/** Result of pulling catalog from the API, including what the sede still lacks for caja. */
data class CatalogSyncReport(
    val productCount: Int,
    val operatorCount: Int,
    val missing: List<String>,
) {
    val isReady: Boolean get() = missing.isEmpty()

    fun userMessage(): String? =
        if (missing.isEmpty()) {
            null
        } else {
            "Para operar en caja aún falta: ${missing.joinToString("; ")}."
        }
}

class ProvisioningRepository(private val context: Context, private val provider: PosDatabaseProvider) {
    private val db get() = provider.database(RuntimeMode.PRODUCTION)
    private val credentials = DeviceCredentials(context)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
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
        credentials.clear()
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
        require(url.startsWith("https://") || (BuildConfig.DEBUG && (url.startsWith("http://10.0.2.2") || url.startsWith("http://localhost")))) { "La URL debe usar HTTPS en release" }
        db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(baseUrl = url, status = "CONFIGURED"))
    }

    suspend fun enroll(baseUrl: String, code: String, name: String): EnrollResponse {
        configureUrl(baseUrl)
        val publicId = prefs.getString("publicId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("publicId", it).apply() }
        val api = retrofit(baseUrl, null)
        val result = api.enroll(EnrollRequest(code, publicId, name, BuildConfig.VERSION_NAME))
        credentials.save(result.accessToken, result.refreshToken)
        val device = DeviceEntity(result.deviceId, name, result.visibleCode, 1, result.site.id, result.status, result.minAppVersion, json.encodeToString(result.eventSchemaVersions))
        db.runInTransaction {
            db.siteDao().clear()
            db.siteDao().insert(SiteEntity(result.site.id, result.site.name, result.site.address, result.site.currency))
            db.operationsDao().insertDevice(device)
            db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(status = "ENROLLED"))
        }
        // Catalog import can fail independently; tokens/device are already persisted for SyncWorker retry.
        try {
            applyBootstrap(retrofit(baseUrl, result.accessToken).bootstrap())
        } catch (e: Exception) {
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(status = "RETRYING", lastError = e.message))
            throw e
        }
        return result
    }

    /**
     * Pulls catalog from the API right now.
     * If local operators/products are incomplete, always re-runs full bootstrap
     * (deltas alone cannot rebuild an empty tablet).
     */
    suspend fun syncCatalogNow(): Result<CatalogSyncReport> {
        val state = db.syncDao().state()
        val baseUrl = state?.baseUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Result.failure(IllegalStateException("Dispositivo no enrolado."))
        val access = credentials.access()
            ?: return Result.failure(IllegalStateException("Sesión del dispositivo inválida. Vuelve a enrolar."))
        return try {
            val api = retrofit(baseUrl, access)
            pullCatalog(api, state)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                val refresh = credentials.refresh()
                    ?: return Result.failure(e)
                try {
                    val tokens = retrofit(baseUrl, null).refresh(RefreshRequest(refresh))
                    credentials.save(tokens.accessToken, tokens.refreshToken)
                    val api = retrofit(baseUrl, tokens.accessToken)
                    pullCatalog(api, db.syncDao().state() ?: state)
                } catch (refreshError: Exception) {
                    resetForReenrollment("La sesión del dispositivo expiró o fue revocada. Vuelve a enrolar.")
                    Result.failure(refreshError)
                }
            } else if (e.code() == 403) {
                resetForReenrollment("Este dispositivo fue revocado. Genera un código nuevo en la Web Central.")
                Result.failure(e)
            } else {
                val current = db.syncDao().state() ?: state
                db.syncDao().saveState(current.copy(status = "RETRYING", lastError = e.message()))
                Result.failure(e)
            }
        } catch (e: Exception) {
            val current = db.syncDao().state() ?: state
            db.syncDao().saveState(current.copy(status = "RETRYING", lastError = e.message))
            Result.failure(e)
        }
    }

    private suspend fun pullCatalog(
        api: DeviceApi,
        state: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity,
    ): Result<CatalogSyncReport> {
        val localIncomplete = db.productDao().count() == 0 || db.userDao().count() == 0
        val cursor = state.changesCursor
        val report =
            if (localIncomplete || cursor.isNullOrBlank()) {
                val snapshot = api.bootstrap()
                applyBootstrap(snapshot)
                reportFromBootstrap(snapshot)
            } else {
                applyChanges(api.changes(cursor))
                reportFromLocal()
            }
        return Result.success(report)
    }

    private fun reportFromBootstrap(snapshot: BootstrapResponse): CatalogSyncReport {
        val missing = buildList {
            if (snapshot.operators.isEmpty()) {
                add("operadores POS con PIN en esta sede")
            }
            if (snapshot.products.isEmpty()) {
                add("productos publicados en el catálogo POS de la sede")
            }
        }
        return CatalogSyncReport(snapshot.products.size, snapshot.operators.size, missing)
    }

    private fun reportFromLocal(): CatalogSyncReport {
        val products = db.productDao().count()
        val operators = db.userDao().count()
        val missing = buildList {
            if (operators == 0) add("operadores POS con PIN en esta sede")
            if (products == 0) add("productos publicados en el catálogo POS de la sede")
        }
        return CatalogSyncReport(products, operators, missing)
    }

    // Applies catalog/operator delta operations atomically and advances the cursor after commit.
    fun applyChanges(changes: ChangesResponse) {
        db.runInTransaction {
            changes.operations.forEach { op ->
                when (op.entity.lowercase()) {
                    "product" -> when (op.op.lowercase()) {
                        "deactivate" -> db.productDao().deleteById(op.id)
                        else -> op.data?.let { db.productDao().insertAll(listOf(json.decodeFromJsonElement(ProductDto.serializer(), it).toProduct())) }
                    }
                    "operator", "user" -> when (op.op.lowercase()) {
                        "deactivate" -> db.userDao().deleteById(op.id)
                        else -> op.data?.let { db.userDao().insertAll(listOf(json.decodeFromJsonElement(OperatorDto.serializer(), it).toUser())) }
                    }
                    "site" -> when (op.op.lowercase()) {
                        "deactivate" -> db.siteDao().deleteById(op.id)
                        else -> op.data?.let { db.siteDao().insert(json.decodeFromJsonElement(SiteDto.serializer(), it).toSite()) }
                    }
                }
            }
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(changesCursor = changes.nextCursor, lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
        }
    }
    private fun ProductDto.toProduct() = ProductEntity(
        id,
        null,
        sku,
        barcode,
        null,
        name,
        saleCategory,
        unit,
        BigDecimal.valueOf(priceCentavos, 2).toPlainString(),
        BigDecimal.valueOf(costCentavos, 2).toPlainString(),
        available,
        stockQuantity.toString(),
        stockMinQuantity.toString(),
        stockPolicy,
        negativeStockLimit,
        null,
    )
    private fun OperatorDto.toUser() = LocalUserEntity(id, displayName, role, pinHash, active)
    private fun SiteDto.toSite() = SiteEntity(id, name, address, currency)

    private fun retrofit(url: String, access: String?): DeviceApi {
        val normalized = if (url.endsWith("/")) url else "$url/"
        val client = okhttp3.OkHttpClient.Builder().apply { if (access != null) addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $access").build()) } }.build()
        return retrofit2.Retrofit.Builder().baseUrl(normalized).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(DeviceApi::class.java)
    }

    fun applyBootstrap(snapshot: BootstrapResponse) {
        db.runInTransaction {
            db.siteDao().clear()
            db.productDao().clear()
            db.userDao().clear()
            db.siteDao().insert(SiteEntity(snapshot.site.id, snapshot.site.name, snapshot.site.address, snapshot.site.currency))
            db.productDao().insertAll(snapshot.products.map { it.toProduct() })
            db.userDao().insertAll(snapshot.operators.map { it.toUser() })
            db.bootstrapDao().insert(BootstrapEntity(snapshot.snapshotId, snapshot.schemaVersion, System.currentTimeMillis()))
            db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(changesCursor = snapshot.cursors.changes, bootstrapSnapshotId = snapshot.snapshotId, status = "ONLINE"))
        }
    }
}
