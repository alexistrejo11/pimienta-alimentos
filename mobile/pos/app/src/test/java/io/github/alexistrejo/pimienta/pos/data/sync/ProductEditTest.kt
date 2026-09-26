package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductEditTest {
    @Test
    fun unchangedSidesAreSkipped() {
        val plan = planProductEdit("Agua", 1500, false, null, "SKU-1", "Agua", 1500, false, "SKU-1")
        assertFalse(plan.rename)
        assertFalse(plan.offer)
    }

    @Test
    fun nameOnlySkipsOffer() {
        val plan = planProductEdit("Agua", 1500, false, null, "SKU-1", "Horchata", 1500, false, "")
        assertTrue(plan.rename)
        assertFalse(plan.offer)
    }

    @Test
    fun priceOrStockSkipsRename() {
        val price = planProductEdit("Agua", 1500, false, "7501", "SKU-1", "Agua", 1800, false, "7501")
        assertFalse(price.rename)
        assertTrue(price.offer)
        val stock = planProductEdit("Agua", 1500, false, null, "SKU-1", "Agua", 1500, true, "SKU-1")
        assertFalse(stock.rename)
        assertTrue(stock.offer)
    }
}

class ProductEditCallsTest {
    @Test
    fun barcodeChangeSkipsOffer() {
        val plan = planProductEdit("Agua", 1500, false, null, "SKU-1", "Agua", 1500, false, "750999")
        assertTrue(plan.rename)
        assertFalse(plan.offer)
        assertEquals("750999", catalogBarcode("750999", "SKU-1"))
        assertEquals(null, catalogBarcode("SKU-1", "SKU-1"))
        assertEquals("SKU-1", scanCode(null, "SKU-1"))
    }

    @Test
    fun nameOnlyPerformsASingleCall() = runBlocking {
        var renames = 0
        var offers = 0
        applyPlannedProductEdit(
            plan = ProductEditPlan(rename = true, offer = false),
            rename = {
                renames += 1
                sample("Horchata")
            },
            offer = {
                offers += 1
                sample("Agua")
            },
            persist = {},
        )
        assertEquals(1, renames)
        assertEquals(0, offers)
    }

    @Test
    fun bothSidesPersistInOrder() = runBlocking {
        val saved = mutableListOf<String>()
        applyPlannedProductEdit(
            plan = ProductEditPlan(rename = true, offer = true),
            rename = { sample("Horchata", 1500) },
            offer = { sample("Horchata", 1800) },
            persist = { saved += "${it.name}:${it.priceCentavos}" },
        )
        assertEquals(listOf("Horchata:1500", "Horchata:1800"), saved)
    }

    private fun sample(name: String, price: Long = 1500) = ProductDto(
        id = "9",
        sku = "INT-1",
        name = name,
        saleCategory = "Bebidas",
        unit = "PIECE",
        priceCentavos = price,
        costCentavos = 0,
        available = true,
        stockQuantity = 0,
        stockMinQuantity = 0,
        stockPolicy = "NOT_CONTROLLED",
    )
}
