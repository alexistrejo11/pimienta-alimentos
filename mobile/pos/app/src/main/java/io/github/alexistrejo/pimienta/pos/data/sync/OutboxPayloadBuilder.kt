package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.CashCountAttemptEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.InventoryMovementEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.PaymentEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ProductEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleCancellationEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleDiscountEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftCloseEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.ShiftEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Builds sync event payload JSON matching backend POS contracts.
object OutboxPayloadBuilder {
    private val json = Json { explicitNulls = true }

    fun shiftOpened(shift: ShiftEntity): String = encode(
        buildJsonObject {
            put("shiftId", shift.id)
            put("cashierOperatorId", shift.cashierId)
            put("openingCashCentavos", shift.openingCashCentavos)
        }
    )

    fun saleConfirmed(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        payment: PaymentEntity,
        discount: SaleDiscountEntity?,
        products: Map<String, ProductEntity>,
    ): String = encode(
        buildJsonObject {
            put("saleId", sale.id)
            put("folio", sale.folio)
            put("cashierOperatorId", sale.cashierId)
            put("grossCentavos", sale.grossCentavos)
            put("discountCentavos", sale.discountCentavos)
            put("totalCentavos", sale.totalCentavos)
            put("status", sale.status)
            put("lines", JsonArray(lines.map { linePayload(it, products[it.productId]) }))
            put("payments", JsonArray(listOf(paymentPayload(sale, payment))))
            put("discount", discount?.let { discountPayload(it) } ?: JsonNull)
        }
    )

    fun saleCancelled(sale: SaleEntity, cancellation: SaleCancellationEntity): String = encode(
        buildJsonObject {
            put("saleId", sale.id)
            put("reason", cancellation.reason)
            put("authorizedByUserId", cancellation.authorizedByUserId)
            put("authorizedByRole", cancellation.authorizedByRole)
        }
    )

    fun wasteRecorded(movement: InventoryMovementEntity, product: ProductEntity, reason: String): String =
        inventoryMovementPayload(movement, product, reason)

    fun restockRecorded(movement: InventoryMovementEntity, product: ProductEntity, reason: String): String =
        inventoryMovementPayload(movement, product, reason)

    fun cashWithdrawalRecorded(withdrawal: CashWithdrawalEntity): String = encode(
        buildJsonObject {
            put("withdrawalId", withdrawal.id)
            put("folio", withdrawal.folio)
            put("amountCentavos", withdrawal.amountCentavos)
            put("reason", withdrawal.reason)
            put("authorizedByUserId", withdrawal.authorizedByUserId)
            put("authorizedByRole", withdrawal.authorizedByRole)
        }
    )

    fun cashCountSubmitted(attempt: CashCountAttemptEntity): String = encode(
        buildJsonObject {
            put("attemptId", attempt.id)
            put("totalCentavos", attempt.totalCentavos)
            put("denominations", attempt.denominations)
        }
    )

    fun shiftClosed(close: ShiftCloseEntity): String = encode(
        buildJsonObject {
            put("cashExpectedCentavos", close.expectedCashCentavos)
            put("countedCashCentavos", close.countedCashCentavos)
            put("differenceCentavos", close.differenceCentavos)
            put("approvedByUserId", close.approvedByUserId)
        }
    )

    private fun inventoryMovementPayload(movement: InventoryMovementEntity, product: ProductEntity, reason: String): String = encode(
        buildJsonObject {
            put("movementId", movement.id)
            put("productId", product.id)
            put("quantity", kotlin.math.abs(movement.quantityDelta))
            put("reason", reason)
        }
    )

    private fun linePayload(line: SaleLineEntity, product: ProductEntity?): JsonObject = buildJsonObject {
        put("lineId", line.id)
        put("productId", line.productId)
        put("productName", line.productName)
        put("saleCategory", line.categoryName)
        put("quantity", line.quantity)
        put("unit", product?.unit ?: "PIECE")
        put("unitPriceCentavos", line.unitPriceCentavos)
        put("subtotalCentavos", line.subtotalCentavos)
        put("stockPolicy", line.stockPolicy)
        put("soldWithNegativeStock", false)
        put("soldWhileUnavailable", product?.available == false)
        if (product?.barcode?.isNotBlank() == true) put("rawBarcode", product.barcode) else put("rawBarcode", JsonNull)
    }

    private fun paymentPayload(sale: SaleEntity, payment: PaymentEntity): JsonObject = buildJsonObject {
        put("paymentId", payment.id)
        put("method", payment.method)
        put("amountCentavos", payment.amountCentavos)
        if (sale.paymentMethod == "CASH") {
            put("tenderedCentavos", sale.tenderedCentavos)
            put("changeCentavos", sale.changeCentavos)
        }
    }

    private fun discountPayload(discount: SaleDiscountEntity): JsonObject = buildJsonObject {
        put("amountCentavos", discount.amountCentavos)
        put("reason", discount.reason)
        put("authorizedByUserId", discount.authorizedByUserId)
        put("authorizedByRole", discount.authorizedByRole)
    }

    private fun encode(body: JsonObject): String = json.encodeToString(JsonObject.serializer(), body)
}
