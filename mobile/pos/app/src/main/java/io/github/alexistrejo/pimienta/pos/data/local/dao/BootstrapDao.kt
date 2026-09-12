package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity

// Tracks which bootstrap snapshots have already been applied.
@Dao
interface BootstrapDao {
    @Insert fun insert(snapshot: BootstrapEntity)
    @Query("SELECT COUNT(*) FROM bootstrap_snapshot WHERE snapshotId = :snapshotId") fun count(snapshotId: String): Int
}
