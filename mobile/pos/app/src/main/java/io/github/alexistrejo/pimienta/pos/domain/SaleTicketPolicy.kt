package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity

// Decides whether a confirmed sale should enqueue an automatic ticket print job.
object SaleTicketPolicy {
    fun shouldQueueAutomaticSaleTicket(
        onlyWhenBarcoded: Boolean,
        lines: List<CartLine>,
        productsById: Map<String, ProductEntity>,
    ): Boolean {
        if (!onlyWhenBarcoded) return true
        return lines.any { lineIncludesBarcodedProduct(it, productsById) }
    }

    internal fun lineIncludesBarcodedProduct(line: CartLine, productsById: Map<String, ProductEntity>): Boolean =
        when (line.lineType) {
            SaleLineType.PENDING_CATALOG -> !line.sourceBarcode.isNullOrBlank()
            SaleLineType.OPEN_AMOUNT -> false
            SaleLineType.CATALOG -> {
                val productId = line.productId ?: return false
                productsById[productId]?.hasDistinctBarcode() == true
            }
        }
}
