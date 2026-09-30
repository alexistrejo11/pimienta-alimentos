package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity

// Decides whether a confirmed sale should enqueue an automatic ticket print job.
object SaleTicketPolicy {
    fun shouldQueueAutomaticSaleTicket(
        kitchenTicketFilterEnabled: Boolean,
        lines: List<CartLine>,
        productsById: Map<String, ProductEntity>,
    ): Boolean {
        if (!kitchenTicketFilterEnabled) return true
        return lines.any { lineRequiresKitchenTicket(it, productsById) }
    }

    // Prepared / internal catalog lines and open amounts need a ticket; packaged goods with supplier barcode do not.
    internal fun lineRequiresKitchenTicket(line: CartLine, productsById: Map<String, ProductEntity>): Boolean =
        when (line.lineType) {
            SaleLineType.OPEN_AMOUNT -> true
            SaleLineType.PENDING_CATALOG -> false
            SaleLineType.CATALOG -> {
                val productId = line.productId ?: return false
                productsById[productId]?.hasDistinctBarcode() != true
            }
        }
}
