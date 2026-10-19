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

    // Turns a chosen discount mode into the charge and the discount, or a block the UI can show.
    fun quote(mode: DiscountMode, lines: Iterable<CartLine>, targetCentavos: Long? = null): DiscountQuote {
        val cart = lines.toList()
        val gross = grossCentavos(cart)
        return when (mode) {
            DiscountMode.FULL_COURTESY ->
                DiscountQuote.Accepted(mode, chargeCentavos = 0L, discountCentavos = gross)
            DiscountMode.AT_COST -> quoteAtCost(cart, gross)
            DiscountMode.TARGET_AMOUNT -> {
                val target = targetCentavos
                if (target == null || target <= 0L || target >= gross) {
                    DiscountQuote.Blocked(DiscountBlock.TARGET_OUT_OF_RANGE)
                } else {
                    DiscountQuote.Accepted(mode, chargeCentavos = target, discountCentavos = Math.subtractExact(gross, target))
                }
            }
        }
    }

    // At-cost needs a positive catalog cost on every line, and that sum must stay under the menu price.
    private fun quoteAtCost(lines: List<CartLine>, gross: Long): DiscountQuote {
        val missing = lines.filter { line ->
            line.lineType != SaleLineType.CATALOG || line.unitCostCentavos == null || line.unitCostCentavos <= 0L
        }
        if (missing.isNotEmpty()) {
            return DiscountQuote.Blocked(DiscountBlock.MISSING_UNIT_COST, missing.map { it.name }.distinct())
        }
        val totalCost = lines.fold(0L) { acc, line ->
            Math.addExact(acc, Math.multiplyExact(line.unitCostCentavos!!, line.quantity.toLong()))
        }
        if (totalCost >= gross) {
            return DiscountQuote.Blocked(DiscountBlock.COST_NOT_BELOW_SALE_PRICE)
        }
        return DiscountQuote.Accepted(
            DiscountMode.AT_COST,
            chargeCentavos = totalCost,
            discountCentavos = Math.subtractExact(gross, totalCost),
        )
    }
}

// How the operator chose a sale-level discount.
enum class DiscountMode { FULL_COURTESY, AT_COST, TARGET_AMOUNT }

// Why cobro al costo or monto a cobrar cannot be applied.
enum class DiscountBlock { MISSING_UNIT_COST, COST_NOT_BELOW_SALE_PRICE, TARGET_OUT_OF_RANGE }

// Either the amounts to persist, or the lines the operator must fix.
sealed class DiscountQuote {
    data class Accepted(val mode: DiscountMode, val chargeCentavos: Long, val discountCentavos: Long) : DiscountQuote()
    data class Blocked(val block: DiscountBlock, val lineNames: List<String> = emptyList()) : DiscountQuote()
}

// Carries the block and the product names so checkout can refuse the mode without guessing.
class DiscountRejectedException(
    val block: DiscountBlock,
    val lineNames: List<String> = emptyList(),
) : IllegalArgumentException(discountBlockMessage(block, lineNames))

// Cart line for an applied discount: the mode the cashier chose, then the motivo.
fun DiscountMode.summaryLabel(): String = when (this) {
    DiscountMode.FULL_COURTESY -> "Cortesía total"
    DiscountMode.AT_COST -> "Cobro al costo"
    DiscountMode.TARGET_AMOUNT -> "Descuento"
}

// Spanish copy for a quote the cashier cannot authorize.
fun discountBlockMessage(block: DiscountBlock, lineNames: List<String> = emptyList()): String = when (block) {
    DiscountBlock.MISSING_UNIT_COST -> "Sin costo configurado: ${lineNames.joinToString(", ")}."
    DiscountBlock.COST_NOT_BELOW_SALE_PRICE ->
        "El costo no es menor al precio de venta. Cobra normal o usa monto a cobrar."
    DiscountBlock.TARGET_OUT_OF_RANGE ->
        "El monto a cobrar debe ser mayor que 0 y menor que la venta."
}
