package io.github.alexistrejo.pimienta.pos.domain

// Single integer-centavo source for cart, checkout, tickets, and persisted sale totals.
object SaleCalculator {
    fun lineSubtotalCentavos(unitPriceCentavos: Long, quantity: Int): Long =
        Math.multiplyExact(unitPriceCentavos, quantity.toLong())

    fun grossCentavos(lines: Iterable<CartLine>): Long =
        lines.fold(0L) { acc, line -> Math.addExact(acc, lineSubtotalCentavos(line.unitPriceCentavos, line.quantity)) }

    fun netCentavos(grossCentavos: Long, discountCentavos: Long): Long {
        require(discountCentavos >= 0L && discountCentavos <= grossCentavos) {
            "El descuento debe estar entre 0 y la venta bruta."
        }
        return Math.subtractExact(grossCentavos, discountCentavos)
    }

    fun changeCentavos(tenderedCentavos: Long, netCentavos: Long): Long =
        Math.subtractExact(tenderedCentavos, netCentavos)

    // A cortesía is a full-sale discount; partial discounts stay cash or card.
    fun isFullCourtesy(grossCentavos: Long, discountCentavos: Long): Boolean =
        grossCentavos > 0L && discountCentavos == grossCentavos
}
