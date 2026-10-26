package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.PaymentEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleDiscountEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.SaleLineEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies that offline sale payloads preserve open-amount facts and stable identifiers.
class OutboxPayloadBuilderTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun openAmountPayloadKeepsGeneratedDescriptionAndPrice() {
        val sale = SaleEntity("sale-1", "T1-0001", "shift-1", "cashier-1", 1250, 0, 1250, "CASH", 1300, 50, 1)
        val line = SaleLineEntity("line-1", sale.id, null, "Monto Abierto · Bebidas", "Bebidas", 1, 1250, 1250, "NOT_CONTROLLED", "OPEN_AMOUNT")
        val payment = PaymentEntity("payment-1", sale.id, "CASH", 1250)

        val body = json.parseToJsonElement(
            OutboxPayloadBuilder.saleConfirmed(sale, listOf(line), payment, null, emptyMap()),
        ).jsonObject
        val payloadLine = body["lines"]!!.jsonArray.single().jsonObject

        assertEquals("OPEN_AMOUNT", payloadLine["lineType"]!!.toString().trim('"'))
        assertEquals("Monto Abierto · Bebidas", payloadLine["productName"]!!.toString().trim('"'))
        assertEquals("Bebidas", payloadLine["saleCategory"]!!.toString().trim('"'))
        assertEquals("1250", payloadLine["unitPriceCentavos"].toString())
        assertEquals("null", payloadLine["authorizedByOperatorId"].toString())
        assertEquals("null", payloadLine["authorizedAt"].toString())
        assertEquals("null", payloadLine["unitCostCentavos"].toString())
    }

    @Test
    fun salePayloadCarriesDiscountModeAndCatalogCost() {
        val sale = SaleEntity("sale-1", "T1-0001", "shift-1", "cashier-1", 4000, 1500, 2500, "CASH", 2500, 0, 1)
        val line = SaleLineEntity("line-1", sale.id, "p1", "Agua", "Bebidas", 1, 4000, 4000, "NOT_CONTROLLED", unitCostCentavos = 800)
        val payment = PaymentEntity("payment-1", sale.id, "CASH", 2500)
        val discount = SaleDiscountEntity("d1", sale.id, 1500, "Personal", "7", "MANAGER", 1, mode = "AT_COST")

        val body = json.parseToJsonElement(
            OutboxPayloadBuilder.saleConfirmed(sale, listOf(line), payment, discount, emptyMap()),
        ).jsonObject
        val payloadLine = body["lines"]!!.jsonArray.single().jsonObject
        val payloadDiscount = body["discount"]!!.jsonObject

        assertEquals("800", payloadLine["unitCostCentavos"].toString())
        assertEquals("AT_COST", payloadDiscount["mode"]!!.toString().trim('"'))
        assertEquals("Personal", payloadDiscount["reason"]!!.toString().trim('"'))
        assertEquals(null, payloadDiscount["authorizedByRole"])
    }

    @Test
    fun eventIdIsTheStableIdempotencyKey() {
        val event = io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity(
            id = "event-123",
            sequence = 7,
            type = "SALE_CONFIRMED",
            aggregateId = "sale-1",
            status = "PENDING",
            createdAtEpochMillis = 1,
        )

        assertEquals(event.id, event.idempotencyKey)
    }
}
