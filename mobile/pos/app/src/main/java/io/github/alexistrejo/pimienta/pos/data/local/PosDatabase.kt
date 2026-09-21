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
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.DeviceEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PaymentEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleDiscountEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PrintJobEntity
import io.github.alexistrejo.pimienta.pos.data.local.dao.OperationsDao
import io.github.alexistrejo.pimienta.pos.data.local.dao.UserDao
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftCloseEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleCancellationEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import io.github.alexistrejo.pimienta.pos.data.local.dao.SyncDao
import io.github.alexistrejo.pimienta.pos.data.local.entity.TelemetryEventEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CatalogCategoryEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity

// Defines the first local schema for catalog bootstrap data.
@Database(
    entities = [
        SiteEntity::class, ProductEntity::class, BootstrapEntity::class, LocalUserEntity::class,
        DeviceEntity::class, ShiftEntity::class, SaleEntity::class, SaleLineEntity::class,
        PaymentEntity::class, SaleDiscountEntity::class, CashWithdrawalEntity::class, InventoryMovementEntity::class, OutboxEventEntity::class, PrintJobEntity::class,
         CashCountAttemptEntity::class, ShiftCloseEntity::class, SaleCancellationEntity::class, SyncStateEntity::class, TelemetryEventEntity::class,
         CatalogCategoryEntity::class, PosPolicyEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun productDao(): ProductDao
    abstract fun bootstrapDao(): BootstrapDao
    abstract fun userDao(): UserDao
    abstract fun operationsDao(): OperationsDao
    abstract fun syncDao(): SyncDao
    abstract fun syncProjectionDao(): io.github.alexistrejo.pimienta.pos.data.local.dao.SyncProjectionDao

    companion object {
        // Builds the SQLite database owned by Room.
        fun create(context: Context, databaseName: String = "pimienta-pos.db"): PosDatabase = Room.databaseBuilder(
            context,
            PosDatabase::class.java,
            databaseName
         ).addMigrations(
            Migrations.V1_TO_V2, Migrations.V2_TO_V3, Migrations.V3_TO_V4, Migrations.V4_TO_V5,
            Migrations.V5_TO_V6, Migrations.V6_TO_V7, Migrations.V7_TO_V8, Migrations.V8_TO_V9,
            Migrations.V9_TO_V10, Migrations.V10_TO_V11, Migrations.V11_TO_V12, Migrations.V12_TO_V13,
            Migrations.V13_TO_V14, Migrations.V14_TO_V15, Migrations.V15_TO_V16,
        // Safety net: if another instance of this file is ever opened, Flows still get invalidated.
        ).enableMultiInstanceInvalidation().build()
    }
}
