package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity

// Provides the first local catalog queries.
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAll(products: List<ProductEntity>)
    @Query("DELETE FROM product") fun clear()
    @Query("DELETE FROM product WHERE id = :id") fun deleteById(id: String)
    @Query("SELECT * FROM product ORDER BY saleCategory, name") fun getAll(): List<ProductEntity>
    @Query("SELECT * FROM product WHERE barcode = :code OR sku = :code OR legacyBarcode = :code LIMIT 1") fun findByCode(code: String): ProductEntity?
    @Query("SELECT * FROM product WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR (barcode IS NOT NULL AND barcode LIKE '%' || :query || '%') ORDER BY saleCategory, name") fun search(query: String): List<ProductEntity>
    @Query("SELECT * FROM product WHERE id = :id LIMIT 1") fun findById(id: String): ProductEntity?
    @Query("SELECT COUNT(*) FROM product") fun count(): Int
    @Query("UPDATE product SET stock = :stock WHERE id = :id") fun updateStock(id: String, stock: String)
}
