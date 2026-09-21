package io.github.alexistrejo.pimienta.pos.data.sync

import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMappingsTest {
    @Test
    fun productDtoMapsCentavosToDecimalText() {
        val entity = ProductDto(
            id = "11",
            sku = "INT-1",
            barcode = null,
            name = "Agua",
            saleCategory = "Bebidas",
            unit = "PIECE",
            priceCentavos = 1500,
            costCentavos = 0,
            available = true,
            stockQuantity = 0,
            stockMinQuantity = 0,
            stockPolicy = "NOT_CONTROLLED",
        ).toProductEntity()
        assertEquals("11", entity.id)
        assertEquals("15.00", entity.price)
        assertEquals("0.00", entity.cost)
        assertNull(entity.barcode)
        assertEquals("NOT_CONTROLLED", entity.stockPolicy)
        assertEquals(1_500L, io.github.alexistrejo.pimienta.pos.domain.Money.fromCatalog(entity.price))
    }

    @Test
    fun operatorAdminRoleIsStoredAsAdminAndLabeledInSpanish() {
        val fromApi = OperatorDto("op-1", "Luis", "admin", "hash", true).toUser()
        val fromLegacy = OperatorDto("op-2", "Luis", "SUPERADMIN", "hash", true).toUser()
        assertEquals("ADMIN", fromApi.role)
        assertEquals("ADMIN", fromLegacy.role)
        assertEquals("Administrador", fromApi.spanishRoleLabel)
        assertEquals("Gerente", LocalUserEntity.spanishRoleLabel("MANAGER"))
        assertEquals("Cajero", LocalUserEntity.spanishRoleLabel("CASHIER"))
    }

    @Test
    fun trainingProductUsesLocalSkuAndCentavos() {
        val entity = trainingProductEntity(
            name = "Jugo",
            saleCategory = "Bebidas",
            salePriceCentavos = 1800,
            barcode = "750111",
            controlledStock = false,
            id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        )
        assertEquals("18.00", entity.price)
        assertEquals("0.00", entity.cost)
        assertEquals("TRN-aaaaaaaa", entity.sku)
        assertEquals("750111", entity.barcode)
        assertEquals("NOT_CONTROLLED", entity.stockPolicy)
        assertEquals("PIECE", entity.unit)
    }
}
