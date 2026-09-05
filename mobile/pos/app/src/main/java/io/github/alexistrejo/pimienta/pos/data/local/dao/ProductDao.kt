package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity

// Provides the first local catalog queries.
@Dao
interface ProductDao {
    @Insert fun insertAll(products: List<ProductEntity>)
    @Query("DELETE FROM product") fun clear()
    @Query("SELECT * FROM product ORDER BY saleCategory, name") fun getAll(): List<ProductEntity>
    @Query("SELECT * FROM product WHERE barcode = :barcode LIMIT 1") fun findByBarcode(barcode: String): ProductEntity?
}
