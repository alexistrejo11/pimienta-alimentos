package io.github.alexistrejo.pimienta.pos.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// confirmSale accepts the shared open-amount name and rejects a category mismatch.
@RunWith(AndroidJUnit4::class)
class OpenAmountConfirmSaleInstrumentedTest {
    private lateinit var context: Context
    private lateinit var provider: PosDatabaseProvider
    private lateinit var repository: PosRepository
    private lateinit var shift: ShiftEntity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        val database = provider.database(RuntimeMode.SANDBOX)
        TrainingBootstrapImporter(context).resetFromTemplate(database)
        val policy = database.syncProjectionDao().policy()!!
        database.syncProjectionDao().insertPolicy(
            policy.copy(allowOpenProducts = true, openAmountCategoriesJson = """["Bebidas"]"""),
        )
        repository = PosRepository(provider, RuntimeMode.SANDBOX)
        shift = repository.openShift("user-debug-cashier", 0L)!!
    }

    @After
    fun tearDown() {
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun confirmSaleAcceptsCategoryOnlyAndConceptNames() {
        val plain = confirm(openLine("Monto Abierto · Bebidas"))
        val described = confirm(openLine("Monto Abierto · Bebidas (5 cucharas)"))

        assertEquals("Monto Abierto · Bebidas", storedName(plain.id))
        assertEquals("Monto Abierto · Bebidas (5 cucharas)", storedName(described.id))
    }

    @Test
    fun confirmSaleRejectsMismatchedCategoryAndEmptyParentheses() {
        val mismatch = repository.confirmSale(
            shift,
            listOf(openLine("Monto Abierto · Snacks")),
            PaymentMethod.EXTERNAL_CARD_MP,
            0,
        )
        val emptyConcept = repository.confirmSale(
            shift,
            listOf(openLine("Monto Abierto · Bebidas ()")),
            PaymentMethod.EXTERNAL_CARD_MP,
            0,
        )

        assertTrue(mismatch.isFailure)
        assertTrue(emptyConcept.isFailure)
        assertEquals(
            "La descripción de monto abierto no coincide con la categoría.",
            mismatch.exceptionOrNull()?.message,
        )
        assertEquals(
            "La descripción de monto abierto no coincide con la categoría.",
            emptyConcept.exceptionOrNull()?.message,
        )
    }

    private fun confirm(line: CartLine) =
        repository.confirmSale(shift, listOf(line), PaymentMethod.EXTERNAL_CARD_MP, 0).getOrThrow()

    private fun storedName(saleId: String) =
        provider.database(RuntimeMode.SANDBOX).operationsDao().linesForSale(saleId).single().productName

    private fun openLine(name: String) = CartLine(
        productId = null,
        name = name,
        category = "Bebidas",
        unitPriceCentavos = 400,
        stockPolicy = "NOT_CONTROLLED",
        quantity = 1,
        lineType = SaleLineType.OPEN_AMOUNT,
        cartLineId = UUID.randomUUID().toString(),
    )
}
