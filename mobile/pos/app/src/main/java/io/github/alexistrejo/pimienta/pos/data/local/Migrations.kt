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
    // Adds immutable sale totals and authorization evidence without losing prior sales.
    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `sale` ADD COLUMN `grossCentavos` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `sale` ADD COLUMN `discountCentavos` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE `sale` SET `grossCentavos` = `totalCentavos`")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale_discount` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `amountCentavos` INTEGER NOT NULL, `reason` TEXT NOT NULL, `authorizedByUserId` TEXT NOT NULL, `authorizedByRole` TEXT NOT NULL, `authorizedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }
    // Adds durable withdrawal facts for cash safeguards during a shift.
    val V3_TO_V4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `cash_withdrawal` (`id` TEXT NOT NULL, `folio` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `cashierId` TEXT NOT NULL, `amountCentavos` INTEGER NOT NULL, `reason` TEXT NOT NULL, `authorizedByUserId` TEXT NOT NULL, `authorizedByRole` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_withdrawal_folio` ON `cash_withdrawal` (`folio`)")
        }
    }

    // Adds local audit records needed by the functional Manager panel.
    val V4_TO_V5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `sale` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'CONFIRMED'")
            database.execSQL("ALTER TABLE `inventory_movement` ADD COLUMN `movementType` TEXT NOT NULL DEFAULT 'SALE'")
            database.execSQL("ALTER TABLE `print_job` ADD COLUMN `documentType` TEXT NOT NULL DEFAULT 'SALE'")
            database.execSQL("ALTER TABLE `print_job` ADD COLUMN `templateVersion` INTEGER NOT NULL DEFAULT 1")
            database.execSQL("CREATE TABLE IF NOT EXISTS `cash_count_attempt` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `cashierId` TEXT NOT NULL, `totalCentavos` INTEGER NOT NULL, `denominations` TEXT NOT NULL, `status` TEXT NOT NULL, `note` TEXT, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `shift_close` (`id` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `expectedCashCentavos` INTEGER NOT NULL, `countedCashCentavos` INTEGER NOT NULL, `differenceCentavos` INTEGER NOT NULL, `approvedByUserId` TEXT NOT NULL, `approvedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale_cancellation` (`id` TEXT NOT NULL, `saleId` TEXT NOT NULL, `shiftId` TEXT NOT NULL, `reason` TEXT NOT NULL, `authorizedByUserId` TEXT NOT NULL, `authorizedByRole` TEXT NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }
    val V5_TO_V6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `device` ADD COLUMN `siteId` TEXT")
            database.execSQL("ALTER TABLE `device` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'SANDBOX'")
            database.execSQL("ALTER TABLE `device` ADD COLUMN `minAppVersion` TEXT")
            database.execSQL("ALTER TABLE `device` ADD COLUMN `schemaVersionsJson` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `schemaVersion` INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `deviceId` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `siteId` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `shiftId` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `occurredAtEpochMillis` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `payloadJson` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `attemptCount` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `nextAttemptAtEpochMillis` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `lastError` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `resultStatus` TEXT")
            database.execSQL("ALTER TABLE `outbox_event` ADD COLUMN `incidentId` TEXT")
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_state` (`id` INTEGER NOT NULL, `baseUrl` TEXT, `changesCursor` TEXT, `bootstrapSnapshotId` TEXT, `lastSuccessfulAtEpochMillis` INTEGER, `lastError` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("INSERT OR IGNORE INTO `sync_state` (`id`, `status`) VALUES (1, 'NOT_CONFIGURED')")
        }
    }
    // Adds the bounded local queue used for best-effort client diagnostics.
    val V6_TO_V7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `telemetry_event` (`id` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `eventType` TEXT NOT NULL, `level` TEXT NOT NULL, `message` TEXT NOT NULL, `stack` TEXT, `occurredAtEpochMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }
}
