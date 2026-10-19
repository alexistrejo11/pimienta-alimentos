package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks cart, discount, change, and Corte Z invariants to the same integer-centavo formulas.
class SaleCalculatorTest {
    @Test
    fun lineSubtotalMultipliesUnitPriceByQuantity() {
        assertEquals(4_050L, SaleCalculator.lineSubtotalCentavos(1_350, 3))
        assertEquals(4_050L, catalog(1_350, 3).subtotalCentavos)
    }

    @Test
    fun grossSumsDistinctLineTypesBeforeDiscount() {
        val lines = listOf(
            catalog(1_500, 2),
            open(4_500),
            pending(2_000),
        )
        assertEquals(9_500L, SaleCalculator.grossCentavos(lines))
        assertEquals(9_500L, lines.totalCentavos())
    }

    @Test
    fun partialDiscountReducesNetAndCashChangeUsesNet() {
        val gross = 10_000L
        val discount = 1_000L
        val net = SaleCalculator.netCentavos(gross, discount)
        assertEquals(9_000L, net)
        assertFalse(SaleCalculator.isFullCourtesy(gross, discount))
        assertEquals(1_000L, SaleCalculator.changeCentavos(tenderedCentavos = 10_000, netCentavos = net))
        assertEquals(0L, SaleCalculator.changeCentavos(tenderedCentavos = net, netCentavos = net))
        assertEquals(-500L, SaleCalculator.changeCentavos(tenderedCentavos = 8_500, netCentavos = net))
    }

    @Test(expected = IllegalArgumentException::class)
    fun netRejectsDiscountAboveGross() {
        SaleCalculator.netCentavos(10_000, 10_001)
    }

    @Test
    fun fullDiscountIsCourtesyAndDoesNotCreateChange() {
        val gross = 8_000L
        assertTrue(SaleCalculator.isFullCourtesy(gross, gross))
        assertEquals(0L, SaleCalculator.netCentavos(gross, gross))
        assertFalse(SaleCalculator.isFullCourtesy(0, 0))
    }

    @Test
    fun typicalShiftDrawerIgnoresCardCourtesyAndCancelledCash() {
        val cashCatalogNet = SaleCalculator.netCentavos(10_000, 1_000)
        val cashOpen = 4_500L
        val cashPending = 2_000L
        val card = 3_000L
        val courtesyGross = 8_000L
        val cancelledCash = 5_000L
        val opening = 50_000L
        val withdrawals = 10_000L

        val cashSales = cashCatalogNet + cashOpen + cashPending
        val expected = ShiftCloseCalculator.expectedCashCentavos(opening, cashSales, withdrawals)
        assertEquals(15_500L, cashSales)
        assertEquals(55_500L, expected)
        assertEquals(-2_000L, ShiftCloseCalculator.differenceCentavos(53_500, expected))

        val gross = 10_000L + cashOpen + cashPending + card + courtesyGross
        val discounts = 1_000L + courtesyGross
        val net = SaleCalculator.netCentavos(gross, discounts)
        val close = ShiftCloseCalculator.breakdown(
            openingCashCentavos = opening,
            cashSalesCentavos = cashSales,
            cardSalesCentavos = card,
            courtesyGrossCentavos = courtesyGross,
            discountsCentavos = discounts,
            grossCentavos = gross,
            netCentavos = net,
            withdrawalsCentavos = withdrawals,
            withdrawalCount = 1,
            cancelledCount = 1,
            cancelledCashCentavos = cancelledCash,
            ticketCount = 5,
            catalogCentavos = 10_000L + card + courtesyGross,
            openAmountCentavos = cashOpen,
            openAmountQuantity = 1,
            pendingCatalogCentavos = cashPending,
            pendingCatalogQuantity = 1,
        )
        assertEquals(55_500L, close.expectedCashCentavos)
        assertTrue(close.commercialMixIsConsistent())
        assertTrue(close.lineMixIsConsistent())
        assertEquals(
            net,
            ShiftCloseCalculator.expectedCashCentavos(0, cashSales, 0) + card,
        )
    }

    @Test
    fun fullCourtesyChargesNothing() {
        val lines = listOf(catalog(2_000, 2))
        val quote = SaleCalculator.quote(DiscountMode.FULL_COURTESY, lines) as DiscountQuote.Accepted
        assertEquals(0L, quote.chargeCentavos)
        assertEquals(4_000L, quote.discountCentavos)
    }

    @Test
    fun atCostChargesTheSumOfPositiveCatalogCosts() {
        val lines = listOf(
            catalog(2_000, 2).copy(unitCostCentavos = 500),
            catalog(1_500, 1).copy(name = "Jugo", unitCostCentavos = 400),
        )
        val quote = SaleCalculator.quote(DiscountMode.AT_COST, lines) as DiscountQuote.Accepted
        assertEquals(1_400L, quote.chargeCentavos)
        assertEquals(4_100L, quote.discountCentavos)
    }

    @Test
    fun atCostNamesLinesWithoutAPositiveCost() {
        val lines = listOf(
            catalog(2_000, 1).copy(name = "Agua", unitCostCentavos = 0),
            open(500).copy(name = "Producto abierto · Snack"),
        )
        val quote = SaleCalculator.quote(DiscountMode.AT_COST, lines) as DiscountQuote.Blocked
        assertEquals(DiscountBlock.MISSING_UNIT_COST, quote.block)
        assertEquals(listOf("Agua", "Producto abierto · Snack"), quote.lineNames)
    }

    @Test
    fun atCostRejectsACostThatDoesNotLowerThePrice() {
        val lines = listOf(catalog(1_000, 1).copy(unitCostCentavos = 1_000))
        val quote = SaleCalculator.quote(DiscountMode.AT_COST, lines) as DiscountQuote.Blocked
        assertEquals(DiscountBlock.COST_NOT_BELOW_SALE_PRICE, quote.block)
    }

    @Test
    fun targetAmountIsWhatTheCustomerPays() {
        val lines = listOf(catalog(4_000, 1))
        val quote = SaleCalculator.quote(DiscountMode.TARGET_AMOUNT, lines, targetCentavos = 3_500) as DiscountQuote.Accepted
        assertEquals(3_500L, quote.chargeCentavos)
        assertEquals(500L, quote.discountCentavos)
    }

    @Test
    fun targetAmountMustStayStrictlyInsideTheSale() {
        val lines = listOf(catalog(4_000, 1))
        assertEquals(DiscountBlock.TARGET_OUT_OF_RANGE, (SaleCalculator.quote(DiscountMode.TARGET_AMOUNT, lines, 0) as DiscountQuote.Blocked).block)
        assertEquals(DiscountBlock.TARGET_OUT_OF_RANGE, (SaleCalculator.quote(DiscountMode.TARGET_AMOUNT, lines, 4_000) as DiscountQuote.Blocked).block)
    }

    @Test
    fun subtractingCancelledCashAgainWouldUnderstateTheDrawer() {
        val opening = 50_000L
        val liveCash = 18_000L
        val cancelled = 5_000L
        val withdrawals = 10_000L
        val correct = ShiftCloseCalculator.expectedCashCentavos(opening, liveCash, withdrawals)
        val doubleCounted = correct - cancelled
        assertEquals(58_000L, correct)
        assertEquals(53_000L, doubleCounted)
    }

    private fun catalog(price: Long, quantity: Int) = CartLine(
        productId = "p-$price",
        name = "Agua",
        category = "Bebidas",
        unitPriceCentavos = price,
        stockPolicy = "NOT_CONTROLLED",
        quantity = quantity,
    )

    private fun open(price: Long) = CartLine(
        productId = null,
        name = "Producto abierto · Snack",
        category = "Snack",
        unitPriceCentavos = price,
        stockPolicy = "NOT_CONTROLLED",
        quantity = 1,
        lineType = SaleLineType.OPEN_AMOUNT,
        cartLineId = "open-$price",
    )

    private fun pending(price: Long) = CartLine(
        productId = null,
        name = "Producto pendiente de catálogo · 999",
        category = "Pendiente de catálogo",
        unitPriceCentavos = price,
        stockPolicy = "UNLIMITED",
        quantity = 1,
        lineType = SaleLineType.PENDING_CATALOG,
        sourceBarcode = "999",
    )

    private fun List<CartLine>.totalCentavos(): Long = SaleCalculator.grossCentavos(this)
}
