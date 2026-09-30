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
    private lateinit var preparedProduct: ProductEntity
    private lateinit var packagedProduct: ProductEntity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PosLocalPreferences(context).setKitchenTicketPrintFilterEnabled(false)
        provider = PosDatabaseProvider(context)
        provider.resetTrainingDatabase()
        TrainingBootstrapImporter(context).resetFromTemplate(provider.database(RuntimeMode.SANDBOX))
        preparedProduct = provider.database(RuntimeMode.SANDBOX).productDao().getAll().first()
        packagedProduct = ProductEntity(
            id = "product-packaged-test",
            sku = "BOING-SKU",
            barcode = "7501234567890",
            name = "Boing",
            saleCategory = "Bebidas",
            unit = "PIECE",
            price = "15.00",
            cost = "0",
            available = true,
            stock = "0",
            stockMin = "0",
            stockPolicy = "NOT_CONTROLLED",
            negativeStockLimit = null,
        )
        provider.database(RuntimeMode.SANDBOX).productDao().insertAll(listOf(packagedProduct))
        repository = PosRepository(provider, RuntimeMode.SANDBOX)
        shift = repository.openShift("user-debug-cashier", 0L)!!
    }

    @After
    fun tearDown() {
        PosLocalPreferences(context).setKitchenTicketPrintFilterEnabled(false)
        provider.close()
        context.deleteDatabase(PosDatabaseProvider.TRAINING_DB_NAME)
    }

    @Test
    fun confirmSale_queuesAutomaticPrintJobWhenFilterDisabled() {
        repository.setKitchenTicketPrintFilterEnabled(false)
        val sale = confirmSale(packagedProduct)

        assertEquals(1, automaticSalePrintJobs(sale.id).size)
    }

    @Test
    fun confirmSale_queuesAutomaticPrintJobWhenFilterEnabledAndPreparedProduct() {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val sale = confirmSale(preparedProduct)

        assertEquals(1, automaticSalePrintJobs(sale.id).size)
    }

    @Test
    fun confirmSale_skipsAutomaticPrintJobWhenFilterEnabledAndOnlyPackagedProduct() {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val sale = confirmSale(packagedProduct, PaymentMethod.EXTERNAL_CARD_MP)

        assertTrue(automaticSalePrintJobs(sale.id).isEmpty())
    }

    @Test
    fun confirmSale_queuesDrawerKickWhenFilterEnabledPackagedOnlyAndCash() {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val tendered = Money.fromCatalog(packagedProduct.price)
        val sale = confirmSale(packagedProduct, PaymentMethod.CASH, tendered)

        assertTrue(automaticSalePrintJobs(sale.id).isEmpty())
        val drawerJobs = repository.pendingPrintJobs().filter {
            it.saleId == sale.id && it.documentType == "DRAWER_KICK" && !it.duplicate
        }
        assertEquals(1, drawerJobs.size)
    }

    @Test
    fun printProcessor_opensDrawerWithoutTicketWhenDrawerKickJob() = runBlocking {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val tendered = Money.fromCatalog(packagedProduct.price)
        confirmSale(packagedProduct, PaymentMethod.CASH, tendered)

        val printer = FakeTicketPrinter()
        val processor = PrintJobProcessor(provider.database(RuntimeMode.SANDBOX), printer)

        assertEquals(PrintDrain.DONE, processor.processNext())
        val bytes = printer.printed.first()
        assertTrue(bytes.contains(0x1B))
        assertTrue(bytes.contains(0x70.toByte()))
    }

    @Test
    fun requestReprint_queuesJobEvenWhenAutomaticFilterWouldSkip() {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val sale = confirmSale(packagedProduct)
        assertTrue(automaticSalePrintJobs(sale.id).isEmpty())

        repository.requestReprint(sale.id)

        val reprints = repository.pendingPrintJobs().filter { it.saleId == sale.id && it.duplicate }
        assertEquals(1, reprints.size)
    }

    @Test
    fun printProcessor_doesNotRunWhenNoJobWasQueued() = runBlocking {
        repository.setKitchenTicketPrintFilterEnabled(true)
        confirmSale(packagedProduct, PaymentMethod.EXTERNAL_CARD_MP)

        val printer = FakeTicketPrinter()
        val processor = PrintJobProcessor(provider.database(RuntimeMode.SANDBOX), printer)

        assertEquals(PrintDrain.IDLE, processor.processNext())
    }

    @Test
    fun printProcessor_printsWhenConfirmSaleQueuedJob() = runBlocking {
        repository.setKitchenTicketPrintFilterEnabled(true)
        val sale = confirmSale(preparedProduct)
        assertEquals(1, automaticSalePrintJobs(sale.id).size)

        val printer = FakeTicketPrinter()
        val processor = PrintJobProcessor(provider.database(RuntimeMode.SANDBOX), printer)

        assertEquals(PrintDrain.DONE, processor.processNext())
        val bytes = printer.printed.first()
        assertTrue(bytes.isNotEmpty())
        assertFalse(repository.pendingPrintJobs().any { it.saleId == sale.id && it.status == "PENDING" })
    }

    private fun confirmSale(
        product: ProductEntity,
        method: PaymentMethod = PaymentMethod.EXTERNAL_CARD_MP,
        tenderedCentavos: Long = 0,
    ) =
        repository.confirmSale(
            shift,
            listOf(catalogLine(product)),
            method,
            tenderedCentavos,
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
