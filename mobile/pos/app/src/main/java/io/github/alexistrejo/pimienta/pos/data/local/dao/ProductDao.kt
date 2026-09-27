package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

// Provides the first local catalog queries.
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAll(products: List<ProductEntity>)
    @Query("DELETE FROM product") fun clear()
    @Query("DELETE FROM product WHERE id = :id") fun deleteById(id: String)
    @Query("SELECT p.id, p.sku, p.barcode, p.name, p.saleCategory, p.unit, p.price, p.cost, p.available, CAST(CAST(p.centralStock AS REAL) + CASE WHEN p.stockPolicy = 'CONTROLLED' THEN COALESCE((SELECT SUM(m.quantityDelta) FROM inventory_movement m JOIN outbox_event e ON e.id = m.syncEventId WHERE m.productId = p.id AND e.status NOT IN ('SYNCED', 'REJECTED')), 0) ELSE 0 END AS TEXT) AS stock, p.stockMin, p.stockPolicy, p.negativeStockLimit, p.centralStock FROM product p ORDER BY p.saleCategory, p.name") fun getAll(): List<ProductEntity>
    @Query("SELECT p.id, p.sku, p.barcode, p.name, p.saleCategory, p.unit, p.price, p.cost, p.available, CAST(CAST(p.centralStock AS REAL) + CASE WHEN p.stockPolicy = 'CONTROLLED' THEN COALESCE((SELECT SUM(m.quantityDelta) FROM inventory_movement m JOIN outbox_event e ON e.id = m.syncEventId WHERE m.productId = p.id AND e.status NOT IN ('SYNCED', 'REJECTED')), 0) ELSE 0 END AS TEXT) AS stock, p.stockMin, p.stockPolicy, p.negativeStockLimit, p.centralStock FROM product p ORDER BY p.saleCategory, p.name") fun observeAll(): Flow<List<ProductEntity>>
    @Query("SELECT p.id, p.sku, p.barcode, p.name, p.saleCategory, p.unit, p.price, p.cost, p.available, CAST(CAST(p.centralStock AS REAL) + CASE WHEN p.stockPolicy = 'CONTROLLED' THEN COALESCE((SELECT SUM(m.quantityDelta) FROM inventory_movement m JOIN outbox_event e ON e.id = m.syncEventId WHERE m.productId = p.id AND e.status NOT IN ('SYNCED', 'REJECTED')), 0) ELSE 0 END AS TEXT) AS stock, p.stockMin, p.stockPolicy, p.negativeStockLimit, p.centralStock FROM product p WHERE p.barcode = :code OR p.sku = :code LIMIT 1") fun findByCode(code: String): ProductEntity?
    @Query("SELECT p.id, p.sku, p.barcode, p.name, p.saleCategory, p.unit, p.price, p.cost, p.available, CAST(CAST(p.centralStock AS REAL) + CASE WHEN p.stockPolicy = 'CONTROLLED' THEN COALESCE((SELECT SUM(m.quantityDelta) FROM inventory_movement m JOIN outbox_event e ON e.id = m.syncEventId WHERE m.productId = p.id AND e.status NOT IN ('SYNCED', 'REJECTED')), 0) ELSE 0 END AS TEXT) AS stock, p.stockMin, p.stockPolicy, p.negativeStockLimit, p.centralStock FROM product p WHERE p.name LIKE '%' || :query || '%' OR p.sku LIKE '%' || :query || '%' OR (p.barcode IS NOT NULL AND p.barcode LIKE '%' || :query || '%') ORDER BY p.saleCategory, p.name") fun search(query: String): List<ProductEntity>
    @Query("SELECT p.id, p.sku, p.barcode, p.name, p.saleCategory, p.unit, p.price, p.cost, p.available, CAST(CAST(p.centralStock AS REAL) + CASE WHEN p.stockPolicy = 'CONTROLLED' THEN COALESCE((SELECT SUM(m.quantityDelta) FROM inventory_movement m JOIN outbox_event e ON e.id = m.syncEventId WHERE m.productId = p.id AND e.status NOT IN ('SYNCED', 'REJECTED')), 0) ELSE 0 END AS TEXT) AS stock, p.stockMin, p.stockPolicy, p.negativeStockLimit, p.centralStock FROM product p WHERE p.id = :id LIMIT 1") fun findById(id: String): ProductEntity?
    @Query("SELECT COUNT(*) FROM product") fun count(): Int
}
