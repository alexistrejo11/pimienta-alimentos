package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Verifies that a cached v18 policy keeps its values and gets a NULL threshold (tablet default applies).
@RunWith(AndroidJUnit4::class)
class PolicyHighSaleMigrationInstrumentedTest {
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
                    object : SupportSQLiteOpenHelper.Callback(18) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE `pos_policy` (`id` INTEGER NOT NULL, `siteId` TEXT NOT NULL, `allowNegativeStock` INTEGER NOT NULL, `allowOpenProducts` INTEGER NOT NULL, `defaultNegativeStockLimit` INTEGER, `staleCatalogWarnHours` INTEGER NOT NULL, `staleCatalogBlockHours` INTEGER NOT NULL, `openAmountCategoriesJson` TEXT NOT NULL, `stockless` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
                            )
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

    @Test
    fun existingPolicyGetsNullThreshold() {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO `pos_policy` VALUES (1, '7', 1, 1, NULL, 24, 72, '[\"Comida\"]', 1)")

        Migrations.V18_TO_V19.migrate(db)

        db.query("SELECT `siteId`, `stockless`, `highSaleWarningCentavos`, `highChangeWarningCentavos` FROM `pos_policy` WHERE `id` = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals("7", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }

    private companion object {
        const val DATABASE_NAME = "policy-high-sale-migration-test.db"
    }
}
