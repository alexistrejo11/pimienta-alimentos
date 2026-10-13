package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Covers the threshold resolution and the soft warning decision used before checkout.
class HighSaleWarningTest {

    private fun policy(threshold: Long?) = PosPolicyEntity(
        siteId = "1",
        allowNegativeStock = true,
        allowOpenProducts = false,
        defaultNegativeStockLimit = null,
        staleCatalogWarnHours = 24,
        staleCatalogBlockHours = 72,
        openAmountCategoriesJson = "[]",
        highSaleWarningCentavos = threshold,
    )

    @Test
    fun missingPolicyUsesDefault() {
        assertEquals(HighSaleWarning.DEFAULT_CENTAVOS, HighSaleWarning.thresholdCentavos(null))
    }

    @Test
    fun missingFieldUsesDefault() {
        assertEquals(HighSaleWarning.DEFAULT_CENTAVOS, HighSaleWarning.thresholdCentavos(policy(null)))
    }

    @Test
    fun zeroDisablesWarning() {
        assertNull(HighSaleWarning.thresholdCentavos(policy(0)))
    }

    @Test
    fun negativeFallsBackToDefault() {
        assertEquals(HighSaleWarning.DEFAULT_CENTAVOS, HighSaleWarning.thresholdCentavos(policy(-5)))
    }

    @Test
    fun customThresholdIsUsed() {
        assertEquals(500_000L, HighSaleWarning.thresholdCentavos(policy(500_000)))
    }

    @Test
    fun warnsWhenNetReachesThreshold() {
        assertTrue(HighSaleWarning.shouldWarn(1_000_000, 1_000_000, null))
        assertTrue(HighSaleWarning.shouldWarn(1_002_500, 1_000_000, null))
    }

    @Test
    fun doesNotWarnBelowThreshold() {
        assertFalse(HighSaleWarning.shouldWarn(999_999, 1_000_000, null))
    }

    @Test
    fun doesNotWarnForCourtesyOrDisabled() {
        assertFalse(HighSaleWarning.shouldWarn(0, 1_000_000, null))
        assertFalse(HighSaleWarning.shouldWarn(5_000_000, null, null))
    }

    @Test
    fun doesNotRepeatForAcknowledgedTotalButDoesWhenTotalChanges() {
        assertFalse(HighSaleWarning.shouldWarn(1_002_500, 1_000_000, 1_002_500))
        assertTrue(HighSaleWarning.shouldWarn(1_005_000, 1_000_000, 1_002_500))
    }
}
