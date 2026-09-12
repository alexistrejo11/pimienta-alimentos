package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Verifies the exact-centavo conversion used by all Phase 1 sale totals.
class MoneyTest {
    @Test fun catalog_price_converts_to_centavos() {
        assertEquals(4_050, Money.fromCatalog("40.50"))
    }

    @Test fun cash_input_rejects_non_numeric_values() {
        assertNull(Money.fromInput("not-money"))
    }

    @Test fun formatted_total_keeps_two_decimal_places() {
        assertEquals("$8.00", Money.format(800))
    }
}
