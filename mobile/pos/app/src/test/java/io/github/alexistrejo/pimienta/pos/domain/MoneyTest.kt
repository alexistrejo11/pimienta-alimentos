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

    @Test fun formatted_amount_groups_thousands() {
        assertEquals("0.00", Money.formatAmount(0))
        assertEquals("0.05", Money.formatAmount(5))
        assertEquals("999.99", Money.formatAmount(99_999))
        assertEquals("1,000.00", Money.formatAmount(100_000))
        assertEquals("10,000.00", Money.formatAmount(1_000_000))
        assertEquals("10,025.00", Money.formatAmount(1_002_500))
        assertEquals("100,000.00", Money.formatAmount(10_000_000))
        assertEquals("1,234,567.89", Money.formatAmount(123_456_789))
        assertEquals("-1,000.00", Money.formatAmount(-100_000))
    }

    @Test fun formatted_total_groups_thousands_with_currency_symbol() {
        assertEquals("$0.00", Money.format(0))
        assertEquals("$999.99", Money.format(99_999))
        assertEquals("$1,000.00", Money.format(100_000))
        assertEquals("$10,000.00", Money.format(1_000_000))
        assertEquals("$10,025.00", Money.format(1_002_500))
        assertEquals("-\$1,000.00", Money.format(-100_000))
    }

    @Test fun grouped_display_text_is_not_accepted_as_input() {
        assertNull(Money.fromInput(Money.formatAmount(100_000)))
        assertNull(Money.fromInput(Money.formatAmount(1_002_500)))
        assertNull(Money.fromInput("1,000.00"))
        assertEquals(5_050L, Money.fromInput("50,50"))
        assertEquals(100_000L, Money.fromInput("1000.00"))
    }

    @Test fun average_ticket_rounds_half_up() {
        assertEquals(0L, Money.averageCentavos(2_001, 0))
        assertEquals(1_001L, Money.averageCentavos(2_001, 2))
        assertEquals(1_000L, Money.averageCentavos(2_000, 2))
    }
}
