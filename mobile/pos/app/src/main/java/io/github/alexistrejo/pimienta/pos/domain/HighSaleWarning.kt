package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity

// Soft "unusually high total" check before checkout. It never blocks or validates a sale.
object HighSaleWarning {
    // Must match PosOperationalConfig.DEFAULT_HIGH_SALE_WARNING_CENTAVOS on the server ($10,000.00).
    const val DEFAULT_CENTAVOS: Long = 1_000_000L

    // Effective threshold from the cached site policy; null means the warning is disabled.
    fun thresholdCentavos(policy: PosPolicyEntity?): Long? {
        val raw = policy?.highSaleWarningCentavos
        return when {
            raw == null || raw < 0 -> DEFAULT_CENTAVOS
            raw == 0L -> null
            else -> raw
        }
    }

    // Warns when the net total reaches the threshold, unless the cashier already accepted this same total.
    fun shouldWarn(netCentavos: Long, thresholdCentavos: Long?, acknowledgedNetCentavos: Long?): Boolean =
        thresholdCentavos != null &&
            netCentavos > 0 &&
            netCentavos >= thresholdCentavos &&
            netCentavos != acknowledgedNetCentavos
}
