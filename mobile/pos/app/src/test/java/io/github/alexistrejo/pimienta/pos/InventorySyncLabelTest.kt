package io.github.alexistrejo.pimienta.pos

import io.github.alexistrejo.pimienta.pos.data.local.entity.SyncStateEntity
import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies that stock freshness messaging remains honest while the tablet is offline.
class InventorySyncLabelTest {
    @Test
    fun onlineStateShowsAgeAndPendingLocalEffects() {
        val state = SyncStateEntity(status = "ONLINE", lastSuccessfulAtEpochMillis = 1_000)

        assertEquals(
            "Base sincronizada hace 2 min · 1 cambio local pendiente",
            inventorySyncLabel(state, 1, 121_000),
        )
    }

    @Test
    fun retryingStateWarnsThatInventoryMayBeStale() {
        val state = SyncStateEntity(status = "RETRYING", lastSuccessfulAtEpochMillis = 1_000)

        assertEquals(
            "Inventario puede estar desactualizado · 2 cambios locales pendientes",
            inventorySyncLabel(state, 2, 121_000),
        )
    }

    @Test
    fun stocklessStateDoesNotClaimInventory() {
        assertEquals("Solo venta · 0 cambios locales pendientes", salesOnlySyncLabel(0))
        assertEquals("Solo venta · 1 cambio local pendiente", salesOnlySyncLabel(1))
    }
}
