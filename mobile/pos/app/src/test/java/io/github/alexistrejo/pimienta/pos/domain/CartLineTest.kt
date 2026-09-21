package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CartLineTest {
    @Test
    fun openAmountLinesWithSameCategoryAndPriceStayDistinct() {
        val first = openLine("a")
        val second = openLine("b")
        assertNotEquals(first.lineKey, second.lineKey)
    }

    @Test
    fun catalogLinesOfTheSameProductShareAKey() {
        val first = CartLine("p1", "Agua", "Bebidas", 1500, "NOT_CONTROLLED", 1)
        val second = CartLine("p1", "Agua", "Bebidas", 1500, "NOT_CONTROLLED", 2)
        assertEquals(first.lineKey, second.lineKey)
        assertEquals(3_000L, second.subtotalCentavos)
    }

    private fun openLine(id: String) = CartLine(
        productId = null,
        name = "Producto abierto · Snack",
        category = "Snack",
        unitPriceCentavos = 4500,
        stockPolicy = "NOT_CONTROLLED",
        quantity = 1,
        lineType = SaleLineType.OPEN_AMOUNT,
        cartLineId = id,
        authorizedByUserId = "user-debug-manager",
        authorizedAtEpochMillis = 1L,
    )
}
