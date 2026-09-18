package io.github.alexistrejo.pimienta.pos.domain

// Holds the Corte Z numbers for one shift: commercial mix plus cash-drawer math.
data class ShiftCloseBreakdown(
    val openingCashCentavos: Long,
    val cashSalesCentavos: Long,
    val cardSalesCentavos: Long,
    val courtesyGrossCentavos: Long,
    val discountsCentavos: Long,
    val grossCentavos: Long,
    val netCentavos: Long,
    val withdrawalsCentavos: Long,
    val withdrawalCount: Int,
    val cancelledCount: Int,
    val cancelledCashCentavos: Long,
    val ticketCount: Int,
    val catalogCentavos: Long,
    val openAmountCentavos: Long,
    val openAmountQuantity: Int,
    val pendingCatalogCentavos: Long,
    val pendingCatalogQuantity: Int,
) {
    val expectedCashCentavos: Long
        get() = ShiftCloseCalculator.expectedCashCentavos(
            openingCashCentavos,
            cashSalesCentavos,
            withdrawalsCentavos,
        )
}

// Computes Corte Z drawer totals without floating-point money.
object ShiftCloseCalculator {
    // Expected drawer = opening float + net cash collected - safeguard withdrawals.
    // Cancelled cash sales are already excluded from cashSalesCentavos by the DAO.
    fun expectedCashCentavos(
        openingCashCentavos: Long,
        cashSalesCentavos: Long,
        withdrawalsCentavos: Long,
    ): Long = openingCashCentavos + cashSalesCentavos - withdrawalsCentavos

    fun differenceCentavos(countedCashCentavos: Long, expectedCashCentavos: Long): Long =
        countedCashCentavos - expectedCashCentavos

    fun breakdown(
        openingCashCentavos: Long,
        cashSalesCentavos: Long,
        cardSalesCentavos: Long,
        courtesyGrossCentavos: Long,
        discountsCentavos: Long,
        grossCentavos: Long,
        netCentavos: Long,
        withdrawalsCentavos: Long,
        withdrawalCount: Int,
        cancelledCount: Int,
        cancelledCashCentavos: Long,
        ticketCount: Int,
        catalogCentavos: Long,
        openAmountCentavos: Long,
        openAmountQuantity: Int,
        pendingCatalogCentavos: Long,
        pendingCatalogQuantity: Int,
    ): ShiftCloseBreakdown = ShiftCloseBreakdown(
        openingCashCentavos = openingCashCentavos,
        cashSalesCentavos = cashSalesCentavos,
        cardSalesCentavos = cardSalesCentavos,
        courtesyGrossCentavos = courtesyGrossCentavos,
        discountsCentavos = discountsCentavos,
        grossCentavos = grossCentavos,
        netCentavos = netCentavos,
        withdrawalsCentavos = withdrawalsCentavos,
        withdrawalCount = withdrawalCount,
        cancelledCount = cancelledCount,
        cancelledCashCentavos = cancelledCashCentavos,
        ticketCount = ticketCount,
        catalogCentavos = catalogCentavos,
        openAmountCentavos = openAmountCentavos,
        openAmountQuantity = openAmountQuantity,
        pendingCatalogCentavos = pendingCatalogCentavos,
        pendingCatalogQuantity = pendingCatalogQuantity,
    )
}
