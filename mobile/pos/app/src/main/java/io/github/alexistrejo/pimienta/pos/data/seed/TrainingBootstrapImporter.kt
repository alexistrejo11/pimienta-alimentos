package io.github.alexistrejo.pimienta.pos.data.seed

import android.content.Context
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

// Clones the packaged training template into a writable scratch database.
class TrainingBootstrapImporter(private val context: Context) {
    // Replaces all master data in the training database from the immutable asset.
    fun resetFromTemplate(database: PosDatabase, assetName: String = ASSET_NAME) {
        val root = try {
            JSONObject(context.assets.open(assetName).bufferedReader().use { it.readText() })
        } catch (_: IOException) {
            throw IllegalStateException("Training bootstrap asset missing: $assetName")
        }
        val snapshotId = root.getString("snapshotId")
        val siteJson = root.getJSONObject("site")
        val site = SiteEntity(
            id = siteJson.getString("id"),
            name = siteJson.getString("name"),
            address = siteJson.optString("address"),
            currency = siteJson.getString("currency"),
        )
        val products = root.getJSONArray("products").let { array ->
            (0 until array.length()).map { index -> array.getJSONObject(index).toProductEntity() }
        }
        val users = root.getJSONArray("users").let { array ->
            (0 until array.length()).map { index -> array.getJSONObject(index).toUserEntity() }
        }
        val deviceJson = root.getJSONObject("device")
        val device = DeviceEntity(
            id = deviceJson.getString("id"),
            name = deviceJson.getString("name"),
            visibleCode = "T1",
            nextEventSequence = 1,
            siteId = site.id,
            status = deviceJson.optString("status", "SANDBOX"),
        )
        val openAmountCategoriesArray = root.optJSONArray("openAmountCategories")
        val openAmountCategoriesList = if (openAmountCategoriesArray != null) {
            (0 until openAmountCategoriesArray.length()).map { openAmountCategoriesArray.getString(it) }
        } else {
            listOf("Apoyo escolar", "Comida", "Bebida", "Snack", "Otro")
        }
        val policy = PosPolicyEntity(
            id = 1,
            siteId = site.id,
            allowNegativeStock = true,
            allowOpenProducts = true,
            defaultNegativeStockLimit = 10,
            staleCatalogWarnHours = 24,
            staleCatalogBlockHours = 72,
            openAmountCategoriesJson = JSONArray(openAmountCategoriesList).toString(),
            stockless = false,
        )

        database.runInTransaction {
            database.siteDao().clear()
            database.productDao().clear()
            database.userDao().clear()
            database.syncProjectionDao().clearPolicy()
            database.siteDao().insert(site)
            database.productDao().insertAll(products)
            database.userDao().insertAll(users)
            database.operationsDao().insertDevice(device)
            database.syncProjectionDao().insertPolicy(policy)
            database.bootstrapDao().insert(
                BootstrapEntity(snapshotId, root.getInt("schemaVersion"), System.currentTimeMillis()),
            )
        }
    }

    private fun JSONObject.toProductEntity(): ProductEntity = ProductEntity(
        id = getString("id"),
        sku = getString("sku"),
        barcode = getString("barcode"),
        name = getString("name"),
        saleCategory = getString("saleCategory"),
        unit = getString("unit"),
        price = getString("price"),
        cost = getString("cost"),
        available = getBoolean("available"),
        stock = getString("stock"),
        stockMin = getString("stockMin"),
        stockPolicy = getString("stockPolicy"),
        negativeStockLimit = null,
    )

    private fun JSONObject.toUserEntity(): LocalUserEntity = LocalUserEntity(
        id = getString("id"),
        displayName = getString("displayName"),
        role = LocalUserEntity.normalizeRole(getString("role")),
        pinHash = getString("pinHash"),
        active = getBoolean("active"),
    )

    companion object {
        const val ASSET_NAME = "pos-training-bootstrap.json"
    }
}
