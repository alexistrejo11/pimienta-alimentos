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

    // Gross − discounts equals net, and net is only cash + card (courtesy contributes 0).
    fun commercialMixIsConsistent(): Boolean =
        netCentavos == ShiftCloseCalculator.commercialNetCentavos(grossCentavos, discountsCentavos) &&
            netCentavos == ShiftCloseCalculator.collectedNetCentavos(cashSalesCentavos, cardSalesCentavos)

    // Catalog + open amount + pending catalog is the pre-discount sale gross.
    fun lineMixIsConsistent(): Boolean =
        grossCentavos == ShiftCloseCalculator.lineGrossCentavos(
            catalogCentavos,
            openAmountCentavos,
            pendingCatalogCentavos,
        )
}

// Computes Corte Z drawer totals without floating-point money.
object ShiftCloseCalculator {
    // Drawer formula: opening + net confirmed cash − sangrías.
    // Cancelled cash is already absent from cashSalesCentavos (DAO excludes CANCELLED), so refunds are not subtracted again.
    fun expectedCashCentavos(
        openingCashCentavos: Long,
        cashSalesCentavos: Long,
        withdrawalsCentavos: Long,
    ): Long = Math.subtractExact(Math.addExact(openingCashCentavos, cashSalesCentavos), withdrawalsCentavos)

    fun differenceCentavos(countedCashCentavos: Long, expectedCashCentavos: Long): Long =
        Math.subtractExact(countedCashCentavos, expectedCashCentavos)

    // Commercial mix: gross − discounts = net = cash + card (courtesy net is always 0).
    fun commercialNetCentavos(grossCentavos: Long, discountsCentavos: Long): Long =
        Math.subtractExact(grossCentavos, discountsCentavos)

    fun collectedNetCentavos(cashSalesCentavos: Long, cardSalesCentavos: Long): Long =
        Math.addExact(cashSalesCentavos, cardSalesCentavos)

    // Line mix is the sale gross before the sale-level discount.
    fun lineGrossCentavos(catalogCentavos: Long, openAmountCentavos: Long, pendingCatalogCentavos: Long): Long =
        Math.addExact(Math.addExact(catalogCentavos, openAmountCentavos), pendingCatalogCentavos)

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
