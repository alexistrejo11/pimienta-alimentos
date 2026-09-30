package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context

// Device-local operational toggles that stay on the tablet and are not synced from the server.
class PosLocalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // When true, automatic sale tickets are queued only if the cart includes a barcoded catalog line.
    fun autoPrintOnlyBarcodedSales(): Boolean = prefs.getBoolean(KEY_AUTO_PRINT_ONLY_BARCODED, false)

    fun setAutoPrintOnlyBarcodedSales(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PRINT_ONLY_BARCODED, enabled).apply()
    }

    private companion object {
        const val PREFS = "pos-local-prefs"
        const val KEY_AUTO_PRINT_ONLY_BARCODED = "auto_print_only_barcoded_sales"
    }
}
