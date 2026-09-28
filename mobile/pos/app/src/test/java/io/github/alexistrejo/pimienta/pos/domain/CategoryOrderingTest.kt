package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies POS categories sort alphabetically regardless of case and accents.
class CategoryOrderingTest {
    @Test
    fun sortsIgnoringCaseAndAccents() {
        val result = sortedCategoryNames(listOf("postres", "Bebidas", "Panadería", "dulces", "Ñoquis", "Mexicano"))
        assertEquals(listOf("Bebidas", "dulces", "Mexicano", "Ñoquis", "Panadería", "postres"), result)
    }

    @Test
    fun dropsBlanksAndCaseInsensitiveDuplicates() {
        val result = sortedCategoryNames(listOf("Deli", " ", "deli", "Bebidas", ""))
        assertEquals(listOf("Bebidas", "Deli"), result)
    }
}
