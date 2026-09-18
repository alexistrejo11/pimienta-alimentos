package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingDatabaseResetTest {
    private lateinit var context: Context
    private lateinit var provider: PosDatabaseProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))
    }

    @After
    fun tearDown() {
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun resetTrainingDatabaseRestoresTemplateAndClearsOperationalData() {
        val database = provider.database(RuntimeMode.SANDBOX)
        val productCount = database.productDao().getAll().size
        val site = database.siteDao().current()!!
        val device = database.operationsDao().device()!!
        val cashier = database.userDao().activeUsers().first()
        database.operationsDao().insertShift(
            ShiftEntity(
                id = "shift-test",
                deviceId = device.id,
                siteId = site.id,
                cashierId = cashier.id,
                openingCashCentavos = 0,
                openedAtEpochMillis = 1,
                status = "OPEN",
                nextFolioNumber = 1,
            ),
        )
        assertEquals("shift-test", database.operationsDao().activeShift()?.id)

        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))

        val resetDatabase = provider.database(RuntimeMode.SANDBOX)
        assertNull(resetDatabase.operationsDao().activeShift())
        assertEquals(productCount, resetDatabase.productDao().getAll().size)
        assertTrue(resetDatabase.userDao().count() >= 2)
    }

    @Test
    fun trainingProductIsWipedOnReset() {
        val repository = io.github.alexistrejo.pimienta.pos.domain.PosRepository(provider, RuntimeMode.SANDBOX)
        val created = repository.createTrainingProduct("Jugo práctica", 1800, "Bebidas", "750111999", false)
        assertTrue(created.isSuccess)
        assertTrue(provider.database(RuntimeMode.SANDBOX).productDao().findByCode("750111999") != null)

        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))

        assertNull(provider.database(RuntimeMode.SANDBOX).productDao().findByCode("750111999"))
    }
}
