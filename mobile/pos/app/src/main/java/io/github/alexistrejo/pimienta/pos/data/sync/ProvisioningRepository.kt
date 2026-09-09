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

class ProvisioningRepository(private val context: Context, private val provider: PosDatabaseProvider) {
    private val db get() = provider.database(RuntimeMode.PRODUCTION)
    private val credentials = DeviceCredentials(context)
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences("pos-device-config", Context.MODE_PRIVATE)

    fun baseUrl(): String? = db.syncDao().state()?.baseUrl
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
        applyBootstrap(retrofit(baseUrl, result.accessToken).bootstrap())
        return result
    }

    // Applies catalog/operator delta operations atomically and advances the cursor after commit.
    fun applyChanges(changes: ChangesResponse) {
        db.runInTransaction {
            changes.operations.forEach { op ->
                when (op.entity.lowercase()) {
                    "product" -> if (op.op == "DELETE") db.productDao().deleteById(op.id) else op.data?.let { db.productDao().insertAll(listOf(json.decodeFromJsonElement(ProductDto.serializer(), it).toProduct())) }
                    "operator", "user" -> if (op.op == "DELETE") db.userDao().deleteById(op.id) else op.data?.let { db.userDao().insertAll(listOf(json.decodeFromJsonElement(OperatorDto.serializer(), it).toUser())) }
                    "site" -> if (op.op == "DELETE") db.siteDao().deleteById(op.id) else op.data?.let { db.siteDao().insert(json.decodeFromJsonElement(SiteDto.serializer(), it).toSite()) }
                }
            }
            val current = db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()
            db.syncDao().saveState(current.copy(changesCursor = changes.nextCursor, lastSuccessfulAtEpochMillis = System.currentTimeMillis(), lastError = null, status = "ONLINE"))
        }
    }
    private fun ProductDto.toProduct() = ProductEntity(id, null, sku, barcode ?: "", null, name, saleCategory, unit, BigDecimal.valueOf(priceCentavos, 2).toPlainString(), BigDecimal.valueOf(costCentavos, 2).toPlainString(), available, stockQuantity, stockMinQuantity, stockPolicy, null)
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
            db.productDao().insertAll(snapshot.products.map { p ->
                ProductEntity(p.id, null, p.sku, p.barcode ?: "", null, p.name, p.saleCategory, p.unit, BigDecimal.valueOf(p.priceCentavos, 2).toPlainString(), BigDecimal.valueOf(p.costCentavos, 2).toPlainString(), p.available, p.stockQuantity, p.stockMinQuantity, p.stockPolicy, null)
            })
            db.userDao().insertAll(snapshot.operators.map { LocalUserEntity(it.id, it.displayName, it.role, it.pinHash, it.active) })
            db.bootstrapDao().insert(BootstrapEntity(snapshot.snapshotId, snapshot.schemaVersion, System.currentTimeMillis()))
            db.syncDao().saveState((db.syncDao().state() ?: io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity()).copy(changesCursor = snapshot.cursors.changes, bootstrapSnapshotId = snapshot.snapshotId, status = "ONLINE"))
        }
    }
}
