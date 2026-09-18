package io.github.alexistrejo.pimienta.pos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

// Locks the Corte Z drawer formula, including open/pending lines and exceptions.
class ShiftCloseCalculatorTest {
    @Test
    fun expectedCashAddsOpeningAndCashThenSubtractsWithdrawals() {
        assertEquals(
            58_000L,
            ShiftCloseCalculator.expectedCashCentavos(
                openingCashCentavos = 50_000,
                cashSalesCentavos = 18_000,
                withdrawalsCentavos = 10_000,
            ),
        )
    }

    @Test
    fun cardAndCourtesyDoNotEnterTheDrawer() {
        val close = trainingShift(
            cashSalesCentavos = 18_000,
            cardSalesCentavos = 3_000,
            courtesyGrossCentavos = 8_000,
        )
        assertEquals(58_000L, close.expectedCashCentavos)
        assertEquals(3_000L, close.cardSalesCentavos)
        assertEquals(8_000L, close.courtesyGrossCentavos)
    }

    @Test
    fun openAmountAndPendingCatalogArePartOfCashWhenSoldAsCash() {
        val close = trainingShift(
            cashSalesCentavos = 10_000 + 4_500 + 2_000 + 1_500,
            catalogCentavos = 11_500,
            openAmountCentavos = 4_500,
            openAmountQuantity = 1,
            pendingCatalogCentavos = 2_000,
            pendingCatalogQuantity = 1,
        )
        assertEquals(58_000L, close.expectedCashCentavos)
        assertEquals(4_500L, close.openAmountCentavos)
        assertEquals(2_000L, close.pendingCatalogCentavos)
    }

    @Test
    fun cancelledCashIsExcludedFromExpectedBecauseDaoReportsNetCash() {
        val close = trainingShift(
            cashSalesCentavos = 18_000,
            cancelledCount = 1,
            cancelledCashCentavos = 5_000,
        )
        assertEquals(58_000L, close.expectedCashCentavos)
        assertEquals(5_000L, close.cancelledCashCentavos)
    }

    @Test
    fun courtesyUsesGrossBecauseNetCourtesyTotalIsAlwaysZero() {
        val close = trainingShift(cashSalesCentavos = 18_000, courtesyGrossCentavos = 8_000, discountsCentavos = 8_000, netCentavos = 21_000)
        assertEquals(8_000L, close.courtesyGrossCentavos)
        assertEquals(21_000L, close.netCentavos)
    }

    @Test
    fun countedShortageIsNegativeDifference() {
        assertEquals(
            -2_000L,
            ShiftCloseCalculator.differenceCentavos(countedCashCentavos = 56_000, expectedCashCentavos = 58_000),
        )
    }

    @Test
    fun emptyShiftKeepsOnlyTheOpeningFloat() {
        val close = ShiftCloseCalculator.breakdown(
            openingCashCentavos = 20_000,
            cashSalesCentavos = 0,
            cardSalesCentavos = 0,
            courtesyGrossCentavos = 0,
            discountsCentavos = 0,
            grossCentavos = 0,
            netCentavos = 0,
            withdrawalsCentavos = 0,
            withdrawalCount = 0,
            cancelledCount = 0,
            cancelledCashCentavos = 0,
            ticketCount = 0,
            catalogCentavos = 0,
            openAmountCentavos = 0,
            openAmountQuantity = 0,
            pendingCatalogCentavos = 0,
            pendingCatalogQuantity = 0,
        )
        assertEquals(20_000L, close.expectedCashCentavos)
    }

    private fun trainingShift(
        cashSalesCentavos: Long,
        cardSalesCentavos: Long = 3_000,
        courtesyGrossCentavos: Long = 8_000,
        discountsCentavos: Long = 8_000,
        netCentavos: Long = 21_000,
        cancelledCount: Int = 1,
        cancelledCashCentavos: Long = 5_000,
        catalogCentavos: Long = 11_500,
        openAmountCentavos: Long = 4_500,
        openAmountQuantity: Int = 1,
        pendingCatalogCentavos: Long = 2_000,
        pendingCatalogQuantity: Int = 1,
    ) = ShiftCloseCalculator.breakdown(
        openingCashCentavos = 50_000,
        cashSalesCentavos = cashSalesCentavos,
        cardSalesCentavos = cardSalesCentavos,
        courtesyGrossCentavos = courtesyGrossCentavos,
        discountsCentavos = discountsCentavos,
        grossCentavos = 29_000,
        netCentavos = netCentavos,
        withdrawalsCentavos = 10_000,
        withdrawalCount = 1,
        cancelledCount = cancelledCount,
        cancelledCashCentavos = cancelledCashCentavos,
        ticketCount = 5,
        catalogCentavos = catalogCentavos,
        openAmountCentavos = openAmountCentavos,
        openAmountQuantity = openAmountQuantity,
        pendingCatalogCentavos = pendingCatalogCentavos,
        pendingCatalogQuantity = pendingCatalogQuantity,
    )
}
