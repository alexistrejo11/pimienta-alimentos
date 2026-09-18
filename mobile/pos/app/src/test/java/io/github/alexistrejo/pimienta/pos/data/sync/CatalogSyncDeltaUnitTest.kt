package io.github.alexistrejo.pimienta.pos.data.sync

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies sequential delta mappings for product creation and property updates.
class CatalogSyncDeltaUnitTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun productDtoDeltaUpdatePreservesIdentityAndUpdatesProperties() {
        // 1. Initial product creation delta
        val productV1 = ProductDto(
            id = "prod-999",
            sku = "SKU-999",
            barcode = "750000001",
            name = "Galleta Integrales",
            saleCategory = "Snacks",
            unit = "PZA",
            priceCentavos = 1800,
            costCentavos = 800,
            available = true,
            stockQuantity = 20,
            stockMinQuantity = 2,
            stockPolicy = "UNLIMITED",
        )
        val entityV1 = productV1.toProductEntity()

        assertEquals("prod-999", entityV1.id)
        assertEquals("Galleta Integrales", entityV1.name)
        assertEquals("18.00", entityV1.price)
        assertEquals("Snacks", entityV1.saleCategory)

        // 2. Secondary update delta (Name adjustment & price increase)
        val productV2 = productV1.copy(
            name = "Galletas Integrales Miel 100g",
            priceCentavos = 2200,
            saleCategory = "Abarrotes",
        )
        val entityV2 = productV2.toProductEntity()

        // Verify ID remains unchanged while updated fields reflect the new delta values.
        assertEquals(entityV1.id, entityV2.id)
        assertEquals("Galletas Integrales Miel 100g", entityV2.name)
        assertEquals("22.00", entityV2.price)
        assertEquals("Abarrotes", entityV2.saleCategory)
    }

    @Test
    fun changeOpJsonSerializationPreservesDeltaPayload() {
        val product = ProductDto(
            id = "prod-100",
            sku = "SKU-100",
            barcode = "750000002",
            name = "Café Americano",
            saleCategory = "Bebidas",
            unit = "PZA",
            priceCentavos = 3500,
            costCentavos = 500,
            available = true,
            stockQuantity = 100,
            stockMinQuantity = 10,
            stockPolicy = "UNLIMITED",
        )
        val payload = json.encodeToJsonElement(ProductDto.serializer(), product)
        val changeOp = ChangeOp(op = "upsert", entity = "product", id = product.id, data = payload)

        val decodedDto = json.decodeFromJsonElement(ProductDto.serializer(), changeOp.data!!)
        val entity = decodedDto.toProductEntity()

        assertEquals("prod-100", entity.id)
        assertEquals("Café Americano", entity.name)
        assertEquals("35.00", entity.price)
    }
}
