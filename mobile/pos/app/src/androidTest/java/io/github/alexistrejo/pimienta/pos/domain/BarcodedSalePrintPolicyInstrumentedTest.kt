package io.github.alexistrejo.pimienta.pos.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alexistrejo.pimienta.pos.data.local.PosDatabaseProvider
import io.github.alexistrejo.pimienta.pos.data.local.PosLocalPreferences
import io.github.alexistrejo.pimienta.pos.data.local.RuntimeMode
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import io.github.alexistrejo.pimienta.pos.data.printing.PrintDrain
import io.github.alexistrejo.pimienta.pos.data.printing.PrintJobProcessor
import io.github.alexistrejo.pimienta.pos.data.seed.TrainingBootstrapImporter
import io.github.alexistrejo.pimienta.pos.hardware.FakeTicketPrinter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// End-to-end checks: confirmSale enqueues (or skips) print jobs; PrintJobProcessor drives the fake printer.
@RunWith(AndroidJUnit4::class)
class BarcodedSalePrintPolicyInstrumentedTest {

    private lateinit var context: Context
    private lateinit var provider: PosDatabaseProvider
    private lateinit var repository: PosRepository
    private lateinit var shift: ShiftEntity
    private lateinit var internalProduct: ProductEntity
    private lateinit var barcodedProduct: ProductEntity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PosLocalPreferences(context).setAutoPrintOnlyBarcodedSales(false)
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))
        internalProduct = provider.database(RuntimeMode.SANDBOX).productDao().getAll().first()
        barcodedProduct = ProductEntity(
            id = "product-barcoded-test",
            sku = "INT-PREP",
            barcode = "7501234567890",
            name = "Preparado al momento",
            saleCategory = "Deli",
            unit = "PIECE",
            price = "40.00",
            cost = "0",
            available = true,
            stock = "0",
            stockMin = "0",
            stockPolicy = "NOT_CONTROLLED",
            negativeStockLimit = null,
        )
        provider.database(RuntimeMode.SANDBOX).productDao().insertAll(listOf(barcodedProduct))
        repository = PosRepository(provider, RuntimeMode.SANDBOX)
        shift = repository.openShift("user-debug-cashier", 0L)!!
    }

    @After
    fun tearDown() {
        PosLocalPreferences(context).setAutoPrintOnlyBarcodedSales(false)
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun confirmSale_queuesAutomaticPrintJobWhenPolicyDisabled() {
        repository.setAutoPrintOnlyBarcodedSales(false)
        val sale = confirmSale(internalProduct)

        assertEquals(1, automaticSalePrintJobs(sale.id).size)
    }

    @Test
    fun confirmSale_skipsAutomaticPrintJobWhenPolicyEnabledAndOnlyInternalSku() {
        repository.setAutoPrintOnlyBarcodedSales(true)
        val sale = confirmSale(internalProduct)

        assertTrue(automaticSalePrintJobs(sale.id).isEmpty())
    }

    @Test
    fun confirmSale_queuesAutomaticPrintJobWhenPolicyEnabledAndDistinctBarcodeInCart() {
        repository.setAutoPrintOnlyBarcodedSales(true)
        val sale = confirmSale(barcodedProduct)

        assertEquals(1, automaticSalePrintJobs(sale.id).size)
    }

    @Test
    fun requestReprint_queuesJobEvenWhenAutomaticPolicyWouldSkip() {
        repository.setAutoPrintOnlyBarcodedSales(true)
        val sale = confirmSale(internalProduct)
        assertTrue(automaticSalePrintJobs(sale.id).isEmpty())

        repository.requestReprint(sale.id)

        val reprints = repository.pendingPrintJobs().filter { it.saleId == sale.id && it.duplicate }
        assertEquals(1, reprints.size)
    }

    @Test
    fun printProcessor_doesNotRunWhenNoJobWasQueued() = runBlocking {
        repository.setAutoPrintOnlyBarcodedSales(true)
        confirmSale(internalProduct)

        val printer = FakeTicketPrinter()
        val processor = PrintJobProcessor(provider.database(RuntimeMode.SANDBOX), printer)

        assertEquals(PrintDrain.IDLE, processor.processNext())
    }

    @Test
    fun printProcessor_printsWhenConfirmSaleQueuedJob() = runBlocking {
        repository.setAutoPrintOnlyBarcodedSales(false)
        val sale = confirmSale(barcodedProduct)
        assertEquals(1, automaticSalePrintJobs(sale.id).size)

        val printer = FakeTicketPrinter()
        val processor = PrintJobProcessor(provider.database(RuntimeMode.SANDBOX), printer)

        assertEquals(PrintDrain.DONE, processor.processNext())
        val bytes = printer.printed.first()
        assertTrue(bytes.isNotEmpty())
        assertFalse(repository.pendingPrintJobs().any { it.saleId == sale.id && it.status == "PENDING" })
    }

    private fun confirmSale(product: ProductEntity) =
        repository.confirmSale(
            shift,
            listOf(catalogLine(product)),
            PaymentMethod.EXTERNAL_CARD_MP,
            tenderedCentavos = 0,
        ).getOrThrow()

    private fun catalogLine(product: ProductEntity) = CartLine(
        productId = product.id,
        name = product.name,
        category = product.saleCategory,
        unitPriceCentavos = Money.fromCatalog(product.price),
        stockPolicy = product.stockPolicy,
        quantity = 1,
    )

    private fun automaticSalePrintJobs(saleId: String) =
        repository.pendingPrintJobs().filter { job ->
            job.saleId == saleId && job.documentType == "SALE" && !job.duplicate
        }
}
