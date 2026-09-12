package io.github.alexistrejo.pimienta.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity

// Reads and replaces the local user projection supplied by debug bootstrap data.
@Dao interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertAll(users: List<LocalUserEntity>)
    @Query("DELETE FROM local_user WHERE id = :id") fun deleteById(id: String)
    @Query("SELECT * FROM local_user WHERE active = 1 ORDER BY displayName") fun activeUsers(): List<LocalUserEntity>
    @Query("SELECT * FROM local_user WHERE id = :id LIMIT 1") fun find(id: String): LocalUserEntity?
    @Query("SELECT COUNT(*) FROM local_user") fun count(): Int
    @Query("DELETE FROM local_user") fun clear()
}
