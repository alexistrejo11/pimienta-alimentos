package io.github.alexistrejo.pimienta.pos.data.seed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingBootstrapImporterTest {
    private lateinit var context: Context
    private lateinit var database: PosDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun resetFromTemplateLoadsTrainingUsersProductsAndSite() {
        TrainingBootstrapImporter(context).resetFromTemplate(database)

        assertEquals("Sede demostración", database.siteDao().current()?.name)
        assertEquals("site-debug-001", database.operationsDao().device()?.siteId)
        assertTrue(database.productDao().getAll().isNotEmpty())
        assertTrue(database.userDao().count() >= 2)
        assertTrue(database.syncProjectionDao().policy()?.allowOpenProducts == true)
        database.openHelper.writableDatabase.query("SELECT snapshotId FROM bootstrap_snapshot LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("training-catalog-001", cursor.getString(0))
        }
    }
}
