package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Stores catalog data with exact decimal values represented as text.
@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey val id: String,
    val legacyId: String?,
    val sku: String,
    val barcode: String?,
    val legacyBarcode: String?,
    val name: String,
    val saleCategory: String,
    val unit: String,
    val price: String,
    val cost: String,
    val available: Boolean,
    val stock: String,
    val stockMin: String,
    val stockPolicy: String,
    val negativeStockLimit: Int?,
    val legacyUpdatedAt: Long?,
    val centralStock: String = stock,
)
