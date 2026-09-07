package io.github.alexistrejo.pimienta.pos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Adds immutable operational tables without changing the Phase 0 catalog projection.
object Migrations {
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `local_user` (`id` TEXT NOT NULL, `displayName` TEXT NOT NULL, `role` TEXT NOT NULL, `pinHash` TEXT NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `device` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `visibleCode` TEXT NOT NULL, `nextEventSequence` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `shift` (`id` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `siteId` TEXT NOT NULL, `cashierId` TEXT NOT NULL, `openingCashCentavos` INTEGER NOT NULL, `openedAtEpochMillis` INTEGER NOT NULL, `status` TEXT NOT NULL, `nextFolioNumber` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shift_deviceId_status` ON `shift` (`deviceId`, `status`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale` (`id` TEXT NOT NULL, `folio` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `cashierId` TEXT NOT NULL, `totalCentavos` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `tenderedCentavos` INTEGER NOT NULL, `changeCentavos` INTEGER NOT NULL, `confirmedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sale_folio` ON `sale` (`folio`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale_line` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `productId` TEXT NOT NULL, `productName` TEXT NOT NULL, `categoryName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPriceCentavos` INTEGER NOT NULL, `subtotalCentavos` INTEGER NOT NULL, `stockPolicy` TEXT NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `payment` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `method` TEXT NOT NULL, `amountCentavos` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_movement` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `productId` TEXT NOT NULL, `quantityDelta` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `outbox_event` (`id` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `type` TEXT NOT NULL, `aggregateId` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_outbox_event_sequence` ON `outbox_event` (`sequence`)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `print_job` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `status` TEXT NOT NULL, `duplicate` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }
}
