package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleTicketPolicyTest {
    private val packaged = product(id = "p1", sku = "SKU-1", barcode = "750111")
    private val prepared = product(id = "p2", sku = "INT-2", barcode = null)
    private val skuAsBarcode = product(id = "p3", sku = "SAME", barcode = "SAME")

    @Test
    fun printsAllSalesWhenFilterDisabled() {
        val lines = listOf(catalogLine(prepared.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = false,
                lines = lines,
                productsById = mapOf(prepared.id to prepared),
            ),
        )
    }

    @Test
    fun printsPreparedCatalogWhenFilterEnabled() {
        val lines = listOf(catalogLine(prepared.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = mapOf(prepared.id to prepared),
            ),
        )
    }

    @Test
    fun skipsPackagedOnlyCartWhenFilterEnabled() {
        val lines = listOf(catalogLine(packaged.id))
        assertFalse(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = mapOf(packaged.id to packaged),
            ),
        )
    }

    @Test
    fun printsWhenCartMixesPackagedAndPrepared() {
        val lines = listOf(catalogLine(prepared.id), catalogLine(packaged.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = mapOf(prepared.id to prepared, packaged.id to packaged),
            ),
        )
    }

    @Test
    fun pendingCatalogOnlyDoesNotQueueWhenFilterEnabled() {
        val lines = listOf(
            CartLine(
                productId = null,
                name = "Pendiente",
                category = "Pendiente",
                unitPriceCentavos = 1000,
                stockPolicy = "UNLIMITED",
                quantity = 1,
                lineType = SaleLineType.PENDING_CATALOG,
                sourceBarcode = "750999",
            ),
        )
        assertFalse(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = emptyMap(),
            ),
        )
    }

    @Test
    fun skuEqualBarcodePrintsWhenFilterEnabled() {
        val lines = listOf(catalogLine(skuAsBarcode.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = mapOf(skuAsBarcode.id to skuAsBarcode),
            ),
        )
    }

    @Test
    fun openAmountPrintsWhenFilterEnabled() {
        val lines = listOf(
            CartLine(
                productId = null,
                name = "Producto abierto · Bebidas",
                category = "Bebidas",
                unitPriceCentavos = 1000,
                stockPolicy = "NOT_CONTROLLED",
                quantity = 1,
                lineType = SaleLineType.OPEN_AMOUNT,
            ),
        )
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                kitchenTicketFilterEnabled = true,
                lines = lines,
                productsById = emptyMap(),
            ),
        )
    }

    private fun catalogLine(productId: String) = CartLine(
        productId = productId,
        name = "Item",
        category = "Bebidas",
        unitPriceCentavos = 1000,
        stockPolicy = "NOT_CONTROLLED",
        quantity = 1,
    )

    private fun product(id: String, sku: String, barcode: String?) = ProductEntity(
        id = id,
        sku = sku,
        barcode = barcode,
        name = "Item",
        saleCategory = "Bebidas",
        unit = "PZA",
        price = "10.00",
        cost = "0",
        available = true,
        stock = "0",
        stockMin = "0",
        stockPolicy = "NOT_CONTROLLED",
        negativeStockLimit = null,
    )
}
