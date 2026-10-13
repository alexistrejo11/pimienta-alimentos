package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnusualChangeWarningTest {

    private val threshold = UnusualChangeWarning.DEFAULT_CENTAVOS

    private fun policy(threshold: Long?) = PosPolicyEntity(
        siteId = "1",
        allowNegativeStock = true,
        allowOpenProducts = false,
        defaultNegativeStockLimit = null,
        staleCatalogWarnHours = 24,
        staleCatalogBlockHours = 72,
        openAmountCategoriesJson = "[]",
        highChangeWarningCentavos = threshold,
    )

    @Test
    fun missingPolicyOrValueUsesDefault() {
        assertEquals(100_000L, UnusualChangeWarning.thresholdCentavos(null))
        assertEquals(100_000L, UnusualChangeWarning.thresholdCentavos(policy(null)))
        assertEquals(100_000L, UnusualChangeWarning.thresholdCentavos(policy(-5)))
    }

    @Test
    fun zeroDisablesAndPositiveIsUsedAsIs() {
        assertNull(UnusualChangeWarning.thresholdCentavos(policy(0)))
        assertEquals(50_000L, UnusualChangeWarning.thresholdCentavos(policy(50_000)))
    }

    @Test
    fun cashWithAbsurdTenderedWarns() {
        assertTrue(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 10_011_100, 4_500, threshold))
    }

    @Test
    fun changeExactlyAtThresholdWarns() {
        assertTrue(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 104_500, 4_500, threshold))
    }

    @Test
    fun changeBelowThresholdDoesNotWarn() {
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 104_499, 4_500, threshold))
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 50_000, 4_500, threshold))
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 4_500, 4_500, threshold))
    }

    @Test
    fun customSiteThresholdIsRespected() {
        assertTrue(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 54_500, 4_500, 50_000))
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 54_499, 4_500, 50_000))
    }

    @Test
    fun disabledThresholdNeverWarns() {
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 10_011_100, 4_500, null))
    }

    @Test
    fun insufficientTenderedDoesNotWarn() {
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CASH, 1_000, 4_500, threshold))
    }

    @Test
    fun nonCashMethodsNeverWarn() {
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.EXTERNAL_CARD_MP, 10_011_100, 4_500, threshold))
        assertFalse(UnusualChangeWarning.shouldWarn(PaymentMethod.CORTESIA, 10_011_100, 4_500, threshold))
    }
}
