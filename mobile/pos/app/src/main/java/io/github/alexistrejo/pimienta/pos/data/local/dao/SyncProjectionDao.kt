package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.CatalogCategoryEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity

// Replaces and reads the locally cached catalog configuration projections.
@Dao
interface SyncProjectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategories(categories: List<CatalogCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPolicy(policy: PosPolicyEntity)

    @Query("DELETE FROM catalog_category")
    fun clearCategories()

    @Query("DELETE FROM pos_policy")
    fun clearPolicy()

    @Query("SELECT * FROM catalog_category WHERE siteId = :siteId AND active = 1 ORDER BY name")
    fun activeCategories(siteId: String): List<CatalogCategoryEntity>

    @Query("SELECT * FROM pos_policy WHERE id = 1")
    fun policy(): PosPolicyEntity?
}
