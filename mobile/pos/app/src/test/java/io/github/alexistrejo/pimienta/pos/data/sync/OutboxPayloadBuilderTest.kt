package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.PaymentEntity
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
        val line = SaleLineEntity("line-1", sale.id, null, "Producto abierto · Bebidas", "Bebidas", 1, 1250, 1250, "NOT_CONTROLLED", "OPEN_AMOUNT")
        val payment = PaymentEntity("payment-1", sale.id, "CASH", 1250)

        val body = json.parseToJsonElement(
            OutboxPayloadBuilder.saleConfirmed(sale, listOf(line), payment, null, emptyMap()),
        ).jsonObject
        val payloadLine = body["lines"]!!.jsonArray.single().jsonObject

        assertEquals("OPEN_AMOUNT", payloadLine["lineType"]!!.toString().trim('"'))
        assertEquals("Producto abierto · Bebidas", payloadLine["productName"]!!.toString().trim('"'))
        assertEquals("Bebidas", payloadLine["saleCategory"]!!.toString().trim('"'))
        assertEquals("1250", payloadLine["unitPriceCentavos"].toString())
        assertEquals("null", payloadLine["authorizedByOperatorId"].toString())
        assertEquals("null", payloadLine["authorizedAt"].toString())
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
