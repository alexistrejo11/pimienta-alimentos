package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers the open-amount name used by the dialog, the cart, and sale confirmation.
class OpenAmountNameTest {
    @Test
    fun blankConceptUsesCategoryOnly() {
        assertEquals("Monto Abierto · Bebidas", formatOpenAmountProductName("Bebidas"))
        assertEquals("Monto Abierto · Bebidas", formatOpenAmountProductName("  Bebidas  ", " \n "))
    }

    @Test
    fun conceptIsWrappedAndWhitespaceCollapsed() {
        assertEquals(
            "Monto Abierto · Bebidas (5 cucharas)",
            formatOpenAmountProductName("Bebidas", "5 cucharas"),
        )
        assertEquals(
            "Monto Abierto · Bebidas (5 cucharas)",
            formatOpenAmountProductName(" Bebidas ", "  5   cucharas\n"),
        )
    }

    @Test
    fun longConceptStaysInside255CharactersAndKeepsTheClosingParenthesis() {
        val name = formatOpenAmountProductName("Bebidas", "x".repeat(400))
        assertTrue(name.length <= OPEN_AMOUNT_PRODUCT_NAME_MAX_LENGTH)
        assertTrue(name.startsWith("Monto Abierto · Bebidas ("))
        assertTrue(name.endsWith(")"))
        assertTrue(openAmountNameMatchesCategory(name, "Bebidas"))
    }

    @Test
    fun nameMatchesCategoryWithOrWithoutConcept() {
        assertTrue(openAmountNameMatchesCategory("Monto Abierto · Bebidas", "Bebidas"))
        assertTrue(openAmountNameMatchesCategory("Monto Abierto · Bebidas (5 cucharas)", "  Bebidas"))
        assertFalse(openAmountNameMatchesCategory("Monto Abierto · Snacks", "Bebidas"))
        assertFalse(openAmountNameMatchesCategory("Monto Abierto · Bebidas ()", "Bebidas"))
        assertFalse(openAmountNameMatchesCategory("Monto Abierto · Bebidas ( )", "Bebidas"))
        assertFalse(openAmountNameMatchesCategory("Producto abierto · Bebidas", "Bebidas"))
    }
}
