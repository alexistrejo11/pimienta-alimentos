package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.PosPolicyEntity

// Soft check for a mistyped "Efectivo recibido": asks to confirm when the cash change reaches the
// site threshold. It never blocks the sale.
object UnusualChangeWarning {
    // $1,000.00 MXN; must match PosOperationalConfig.DEFAULT_HIGH_CHANGE_WARNING_CENTAVOS on the server.
    const val DEFAULT_CENTAVOS: Long = 100_000L

    // Effective threshold from the cached site policy; null means the warning is disabled.
    fun thresholdCentavos(policy: PosPolicyEntity?): Long? {
        val raw = policy?.highChangeWarningCentavos
        return when {
            raw == null || raw < 0 -> DEFAULT_CENTAVOS
            raw == 0L -> null
            else -> raw
        }
    }

    fun shouldWarn(method: PaymentMethod, tenderedCentavos: Long, netCentavos: Long, thresholdCentavos: Long?): Boolean =
        thresholdCentavos != null &&
            method == PaymentMethod.CASH &&
            tenderedCentavos >= netCentavos &&
            SaleCalculator.changeCentavos(tenderedCentavos, netCentavos) >= thresholdCentavos
}
