package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Exercises the inventory-only portion of the legacy schema without requiring exported Room schemas.
@RunWith(AndroidJUnit4::class)
class InventoryMigrationInstrumentedTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(13) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE `product` (`id` TEXT NOT NULL PRIMARY KEY, `stock` TEXT NOT NULL, `stockPolicy` TEXT NOT NULL)")
                            db.execSQL("CREATE TABLE `inventory_movement` (`id` TEXT NOT NULL PRIMARY KEY, `saleId` TEXT NOT NULL, `productId` TEXT NOT NULL, `quantityDelta` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, `movementType` TEXT NOT NULL)")
                            db.execSQL("CREATE TABLE `outbox_event` (`id` TEXT NOT NULL PRIMARY KEY, `sequence` INTEGER NOT NULL, `type` TEXT NOT NULL, `aggregateId` TEXT NOT NULL, `status` TEXT NOT NULL)")
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    // A pending deduction was already included in legacy stock and must not be applied twice.
    @Test
    fun pendingLegacyMovementPreservesEffectiveStock() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO `product` (`id`, `stock`, `stockPolicy`) VALUES ('p1', '8', 'CONTROLLED')")
        db.execSQL("INSERT INTO `inventory_movement` VALUES ('m1', 'sale-1', 'p1', -2, 1, 'SALE')")
        db.execSQL("INSERT INTO `outbox_event` VALUES ('event-1', 1, 'SALE_CONFIRMED', 'sale-1', 'PENDING')")

        Migrations.V13_TO_V14.migrate(db)
        Migrations.V14_TO_V15.migrate(db)

        db.query("SELECT `centralStock` FROM `product` WHERE `id` = 'p1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(10, cursor.getString(0).toBigDecimal().toInt())
        }
        db.query("SELECT `syncEventId` FROM `inventory_movement` WHERE `id` = 'm1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("event-1", cursor.getString(0))
        }
    }

    private companion object {
        const val DATABASE_NAME = "inventory-migration-test.db"
    }
}
