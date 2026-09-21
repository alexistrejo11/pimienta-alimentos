package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Verifies the exact-centavo conversion used by all Phase 1 sale totals.
class MoneyTest {
    @Test fun catalog_price_converts_to_centavos() {
        assertEquals(4_050L, Money.fromCatalog("40.50"))
        assertEquals(1_500L, Money.fromCatalog("15.00"))
        assertEquals(50L, Money.fromCatalog("0.50"))
    }

    @Test fun cash_input_rejects_non_numeric_values() {
        assertNull(Money.fromInput("not-money"))
        assertNull(Money.fromInput(""))
        assertNull(Money.fromInput("."))
        assertNull(Money.fromInput("-10"))
        assertNull(Money.fromInput("1e2"))
        assertNull(Money.fromInput("1.999"))
    }

    @Test fun cash_input_accepts_one_or_two_decimals_and_comma() {
        assertEquals(5_000L, Money.fromInput("50"))
        assertEquals(5_050L, Money.fromInput("50.5"))
        assertEquals(5_050L, Money.fromInput("50.50"))
        assertEquals(5_050L, Money.fromInput("50,50"))
        assertEquals(50L, Money.fromInput("0.50"))
        assertEquals(5_050L, Money.fromInput(" 50.50 "))
        assertEquals(5_000L, Money.fromInput("50."))
        assertEquals(1L, Money.fromInput("0.01"))
        assertEquals(1_500L, Money.fromCatalog(java.math.BigDecimal.valueOf(1_500L, 2).toPlainString()))
    }

    @Test fun formatted_total_keeps_two_decimal_places() {
        assertEquals("$8.00", Money.format(800))
        assertEquals("$0.05", Money.format(5))
        assertEquals("-\$20.50", Money.format(-2_050))
        assertEquals("-0.50", Money.formatAmount(-50))
        assertEquals("-20.50", Money.formatAmount(-2_050))
    }

    @Test fun average_ticket_rounds_half_up() {
        assertEquals(0L, Money.averageCentavos(2_001, 0))
        assertEquals(1_001L, Money.averageCentavos(2_001, 2))
        assertEquals(1_000L, Money.averageCentavos(2_000, 2))
    }
}
