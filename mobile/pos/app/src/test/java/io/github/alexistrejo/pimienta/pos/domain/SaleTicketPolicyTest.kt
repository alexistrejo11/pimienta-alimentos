package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleTicketPolicyTest {
    private val barcoded = product(id = "p1", sku = "SKU-1", barcode = "750111")
    private val internalOnly = product(id = "p2", sku = "INT-2", barcode = null)
    private val skuAsBarcode = product(id = "p3", sku = "SAME", barcode = "SAME")

    @Test
    fun printsAllSalesWhenPolicyDisabled() {
        val lines = listOf(catalogLine(internalOnly.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                onlyWhenBarcoded = false,
                lines = lines,
                productsById = mapOf(internalOnly.id to internalOnly),
            ),
        )
    }

    @Test
    fun skipsInternalCatalogWhenPolicyEnabled() {
        val lines = listOf(catalogLine(internalOnly.id))
        assertFalse(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                onlyWhenBarcoded = true,
                lines = lines,
                productsById = mapOf(internalOnly.id to internalOnly),
            ),
        )
    }

    @Test
    fun printsWhenCartIncludesDistinctBarcodeProduct() {
        val lines = listOf(catalogLine(internalOnly.id), catalogLine(barcoded.id))
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                onlyWhenBarcoded = true,
                lines = lines,
                productsById = mapOf(internalOnly.id to internalOnly, barcoded.id to barcoded),
            ),
        )
    }

    @Test
    fun pendingCatalogLineCountsAsBarcoded() {
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
        assertTrue(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                onlyWhenBarcoded = true,
                lines = lines,
                productsById = emptyMap(),
            ),
        )
    }

    @Test
    fun skuEqualBarcodeDoesNotQualify() {
        val lines = listOf(catalogLine(skuAsBarcode.id))
        assertFalse(
            SaleTicketPolicy.shouldQueueAutomaticSaleTicket(
                onlyWhenBarcoded = true,
                lines = lines,
                productsById = mapOf(skuAsBarcode.id to skuAsBarcode),
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
