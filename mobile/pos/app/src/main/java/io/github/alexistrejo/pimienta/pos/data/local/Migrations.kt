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

    // Adds durable print-attempt diagnostics without changing historical documents.
    val V7_TO_V8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `print_job` ADD COLUMN `attemptCount` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `print_job` ADD COLUMN `lastAttemptAtEpochMillis` INTEGER")
            database.execSQL("ALTER TABLE `print_job` ADD COLUMN `lastError` TEXT")
        }
    }

    // Makes supplier barcodes optional and stores the effective negative-stock limit.
    val V8_TO_V9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `product_new` (`id` TEXT NOT NULL, `legacyId` TEXT, `sku` TEXT NOT NULL, `barcode` TEXT, `legacyBarcode` TEXT, `name` TEXT NOT NULL, `saleCategory` TEXT NOT NULL, `unit` TEXT NOT NULL, `price` TEXT NOT NULL, `cost` TEXT NOT NULL, `available` INTEGER NOT NULL, `stock` TEXT NOT NULL, `stockMin` TEXT NOT NULL, `stockPolicy` TEXT NOT NULL, `negativeStockLimit` INTEGER, `legacyUpdatedAt` INTEGER, PRIMARY KEY(`id`))")
            database.execSQL("INSERT INTO `product_new` (`id`, `legacyId`, `sku`, `barcode`, `legacyBarcode`, `name`, `saleCategory`, `unit`, `price`, `cost`, `available`, `stock`, `stockMin`, `stockPolicy`, `negativeStockLimit`, `legacyUpdatedAt`) SELECT `id`, `legacyId`, `sku`, NULLIF(`barcode`, ''), `legacyBarcode`, `name`, `saleCategory`, `unit`, `price`, `cost`, `available`, `stock`, `stockMin`, `stockPolicy`, NULL, `legacyUpdatedAt` FROM `product`")
            database.execSQL("DROP TABLE `product`")
            database.execSQL("ALTER TABLE `product_new` RENAME TO `product`")
        }
    }

    // Allows pending-catalog sale lines without a product id and stores their source barcode.
    val V9_TO_V10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sale_line_new` (
                    `id` TEXT NOT NULL,
                    `saleId` TEXT NOT NULL,
                    `productId` TEXT,
                    `productName` TEXT NOT NULL,
                    `categoryName` TEXT NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `unitPriceCentavos` INTEGER NOT NULL,
                    `subtotalCentavos` INTEGER NOT NULL,
                    `stockPolicy` TEXT NOT NULL,
                    `lineType` TEXT NOT NULL,
                    `sourceBarcode` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO `sale_line_new`
                (`id`, `saleId`, `productId`, `productName`, `categoryName`, `quantity`, `unitPriceCentavos`, `subtotalCentavos`, `stockPolicy`, `lineType`, `sourceBarcode`)
                SELECT `id`, `saleId`, `productId`, `productName`, `categoryName`, `quantity`, `unitPriceCentavos`, `subtotalCentavos`, `stockPolicy`, 'CATALOG', NULL
                FROM `sale_line`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `sale_line`")
            database.execSQL("ALTER TABLE `sale_line_new` RENAME TO `sale_line`")
        }
    }

    // Adds durable catalog categories and POS policies without touching offline sales or outbox data.
    val V10_TO_V11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `catalog_category` (`siteId` TEXT NOT NULL, `name` TEXT NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`siteId`, `name`))",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `pos_policy` (`id` INTEGER NOT NULL, `siteId` TEXT NOT NULL, `allowNegativeStock` INTEGER NOT NULL, `allowOpenProducts` INTEGER NOT NULL, `defaultNegativeStockLimit` INTEGER, `staleCatalogWarnHours` INTEGER NOT NULL, `staleCatalogBlockHours` INTEGER NOT NULL, `openAmountCategoriesJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
            )
        }
    }

    // Renames retry lifecycle values while preserving every queued offline operation.
    val V11_TO_V12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE `outbox_event` SET `status` = 'FAILED_RETRYABLE' WHERE `status` = 'RETRY'")
            database.execSQL("UPDATE `outbox_event` SET `status` = 'FAILED_RETRYABLE' WHERE `status` = 'IN_FLIGHT'")
        }
    }

    // Adds nullable authorization evidence without rewriting historical sale lines.
    val V12_TO_V13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `sale_line` ADD COLUMN `authorizedByOperatorId` INTEGER")
            database.execSQL("ALTER TABLE `sale_line` ADD COLUMN `authorizedAtEpochMillis` INTEGER")
        }
    }

    // Keeps the server snapshot separate from unsynced local stock effects.
    val V13_TO_V14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `product` ADD COLUMN `centralStock` TEXT NOT NULL DEFAULT '0'")
            database.execSQL("UPDATE `product` SET `centralStock` = `stock`")
        }
    }

    // Links each stock movement to its exact outbox event for deterministic reconciliation.
    val V14_TO_V15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `inventory_movement` ADD COLUMN `syncEventId` TEXT")
            database.execSQL("""
                UPDATE `inventory_movement`
                SET `syncEventId` = (
                    SELECT e.id FROM `outbox_event` e
                    WHERE e.aggregateId = `inventory_movement`.saleId
                      AND e.type = CASE `inventory_movement`.movementType
                        WHEN 'SALE' THEN 'SALE_CONFIRMED'
                        WHEN 'SALE_CANCELLATION' THEN 'SALE_CANCELLED'
                        WHEN 'WASTE' THEN 'WASTE_RECORDED'
                        WHEN 'RESTOCK' THEN 'RESTOCK_RECORDED'
                      END
                    ORDER BY e.sequence DESC LIMIT 1
                )
            """.trimIndent())
            database.execSQL("""
                UPDATE `product`
                SET `centralStock` = CAST(
                    CAST(`stock` AS REAL) - COALESCE((
                        SELECT SUM(m.quantityDelta)
                        FROM `inventory_movement` m
                        JOIN `outbox_event` e ON e.id = m.syncEventId
                        WHERE m.productId = `product`.id
                          AND e.status NOT IN ('SYNCED', 'REJECTED')
                    ), 0)
                    AS TEXT
                )
                WHERE `stockPolicy` = 'CONTROLLED'
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_movement_productId` ON `inventory_movement` (`productId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_movement_syncEventId` ON `inventory_movement` (`syncEventId`)")
        }
    }

    // Replaces the unique constraint on (deviceId, status) with a non-unique index so multiple closed shifts can exist for a device.
    val V15_TO_V16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP INDEX IF EXISTS `index_shift_deviceId_status`")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_deviceId_status` ON `shift` (`deviceId`, `status`)")
        }
    }

    // Stores the HQ sales-only flag so the tablet can skip local stock movements.
    val V16_TO_V17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `pos_policy` ADD COLUMN `stockless` INTEGER NOT NULL DEFAULT 0")
        }
    }

    // Drops unused catalog provenance columns that never belonged to the Device API product shape.
    val V17_TO_V18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `product_new` (
                    `id` TEXT NOT NULL,
                    `sku` TEXT NOT NULL,
                    `barcode` TEXT,
                    `name` TEXT NOT NULL,
                    `saleCategory` TEXT NOT NULL,
                    `unit` TEXT NOT NULL,
                    `price` TEXT NOT NULL,
                    `cost` TEXT NOT NULL,
                    `available` INTEGER NOT NULL,
                    `stock` TEXT NOT NULL,
                    `stockMin` TEXT NOT NULL,
                    `stockPolicy` TEXT NOT NULL,
                    `negativeStockLimit` INTEGER,
                    `centralStock` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO `product_new` (
                    `id`, `sku`, `barcode`, `name`, `saleCategory`, `unit`, `price`, `cost`,
                    `available`, `stock`, `stockMin`, `stockPolicy`, `negativeStockLimit`, `centralStock`
                )
                SELECT
                    `id`, `sku`, `barcode`, `name`, `saleCategory`, `unit`, `price`, `cost`,
                    `available`, `stock`, `stockMin`, `stockPolicy`, `negativeStockLimit`, `centralStock`
                FROM `product`
                """.trimIndent()
            )
            database.execSQL("DROP TABLE `product`")
            database.execSQL("ALTER TABLE `product_new` RENAME TO `product`")
        }
    }
}
