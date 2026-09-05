package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.alexistrejo.pimienta.pos.data.local.dao.BootstrapDao
import io.github.alexistrejo.pimienta.pos.data.local.dao.ProductDao
import io.github.alexistrejo.pimienta.pos.data.local.dao.SiteDao
import io.github.alexistrejo.pimienta.pos.data.local.entity.BootstrapEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SiteEntity

// Defines the first local schema for catalog bootstrap data.
@Database(entities = [SiteEntity::class, ProductEntity::class, BootstrapEntity::class], version = 1, exportSchema = false)
abstract class PosDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun productDao(): ProductDao
    abstract fun bootstrapDao(): BootstrapDao

    companion object {
        // Builds the SQLite database owned by Room.
        fun create(context: Context): PosDatabase = Room.databaseBuilder(
            context,
            PosDatabase::class.java,
            "pimienta-pos.db"
        ).build()
    }
}
