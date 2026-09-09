package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity

// Replaces the current site projection.
@Dao
interface SiteDao {
    @Insert fun insert(site: SiteEntity)
    @Query("DELETE FROM site") fun clear()
    @Query("DELETE FROM site WHERE id = :id") fun deleteById(id: String)
}
