package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Stores catalog data with exact decimal values represented as text.
@Entity(tableName = "product")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val barcode: String?,
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
    val centralStock: String = stock,
) {
    // Checks if the product has a distinct supplier barcode (not blank and not equal to the SKU).
    fun hasDistinctBarcode(): Boolean {
        val trimmed = barcode?.trim() ?: return false
        return trimmed.isNotEmpty() && !trimmed.equals(sku.trim(), ignoreCase = true)
    }
}
