package io.github.alexistrejo.pimienta.pos.data.seed

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import java.util.concurrent.Executors
import java.io.IOException
import org.json.JSONObject

// Imports the debug bootstrap once without blocking application startup.
class DebugBootstrapImporter(private val context: Context, private val database: PosDatabase) {
    fun importIfNeeded() {
        Executors.newSingleThreadExecutor().execute {
            val root = try {
                JSONObject(context.assets.open("pos-bootstrap.json").bufferedReader().use { it.readText() })
            } catch (_: IOException) {
                // Release builds do not package the sandbox bootstrap asset.
                return@execute
            }
            val snapshotId = root.getString("snapshotId")
            if (database.bootstrapDao().count(snapshotId) > 0 && database.userDao().count() > 0 && database.operationsDao().device() != null) return@execute

            val siteJson = root.getJSONObject("site")
            val site = SiteEntity(
                id = siteJson.getString("id"),
                name = siteJson.getString("name"),
                address = siteJson.optString("address"),
                currency = siteJson.getString("currency")
            )
            val products = root.getJSONArray("products").let { array ->
                (0 until array.length()).map { index -> array.getJSONObject(index).toProductEntity() }
            }
            val users = root.getJSONArray("users").let { array ->
                (0 until array.length()).map { index -> array.getJSONObject(index).toUserEntity() }
            }
            val deviceJson = root.getJSONObject("device")
            val device = DeviceEntity(
                id = deviceJson.getString("id"), name = deviceJson.getString("name"),
                visibleCode = "T1", nextEventSequence = 1
            )

            // Site, catalog, and applied-snapshot marker commit atomically.
            database.runInTransaction {
                database.siteDao().clear()
                database.productDao().clear()
                database.siteDao().insert(site)
                database.productDao().insertAll(products)
                database.userDao().insertAll(users)
                database.operationsDao().insertDevice(device)
                database.bootstrapDao().insert(BootstrapEntity(snapshotId, root.getInt("schemaVersion"), System.currentTimeMillis()))
            }
        }
    }

    // Converts one JSON product into the local Room representation.
    private fun JSONObject.toProductEntity(): ProductEntity = ProductEntity(
        id = getString("id"),
        legacyId = optNullableString("legacyId"),
        sku = getString("sku"),
        barcode = getString("barcode"),
        legacyBarcode = optNullableString("legacyBarcode"),
        name = getString("name"),
        saleCategory = getString("saleCategory"),
        unit = getString("unit"),
        price = getString("price"),
        cost = getString("cost"),
        available = getBoolean("available"),
        stock = getString("stock"),
        stockMin = getString("stockMin"),
        stockPolicy = getString("stockPolicy"),
        legacyUpdatedAt = if (has("legacyUpdatedAt")) getLong("legacyUpdatedAt") else null
    )

    private fun JSONObject.optNullableString(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null

    // Converts a sandbox user snapshot that contains a PIN verifier rather than a PIN.
    private fun JSONObject.toUserEntity(): LocalUserEntity = LocalUserEntity(
        id = getString("id"), displayName = getString("displayName"), role = getString("role"),
        pinHash = getString("pinHash"), active = getBoolean("active")
    )
}
