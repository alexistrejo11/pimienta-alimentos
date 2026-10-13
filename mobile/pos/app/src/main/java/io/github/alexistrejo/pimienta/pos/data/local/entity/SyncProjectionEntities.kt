package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Stores the active POS categories used by the local catalog and open-amount flow.
@Entity(tableName = "catalog_category", primaryKeys = ["siteId", "name"])
data class CatalogCategoryEntity(
    val siteId: String,
    val name: String,
    val active: Boolean = true,
)

// Stores the current policy projection independently from products.
@Entity(tableName = "pos_policy")
data class PosPolicyEntity(
    @PrimaryKey val id: Int = 1,
    val siteId: String,
    val allowNegativeStock: Boolean,
    val allowOpenProducts: Boolean,
    val defaultNegativeStockLimit: Int?,
    val staleCatalogWarnHours: Int,
    val staleCatalogBlockHours: Int,
    val openAmountCategoriesJson: String,
    // When true, sales do not write local inventory movements.
    val stockless: Boolean = false,
    // Site soft-warning threshold for the net sale total; null means "use the default", 0 disables it.
    val highSaleWarningCentavos: Long? = null,
    // Site soft-warning threshold for cash change; null means "use the default", 0 disables it.
    val highChangeWarningCentavos: Long? = null,
)
