package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context

// Device-local operational toggles that stay on the tablet and are not synced from the server.
class PosLocalPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // When true, skip automatic tickets for carts that are only packaged products (distinct supplier barcode).
    fun kitchenTicketPrintFilterEnabled(): Boolean = prefs.getBoolean(KEY_KITCHEN_TICKET_PRINT_FILTER, false)

    fun setKitchenTicketPrintFilterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KITCHEN_TICKET_PRINT_FILTER, enabled).apply()
    }

    // When true, catalog tiles show names in uppercase; stored catalog names are never changed.
    fun salesCardNamesUppercase(): Boolean = prefs.getBoolean(KEY_SALES_CARD_NAMES_UPPERCASE, false)

    fun setSalesCardNamesUppercase(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SALES_CARD_NAMES_UPPERCASE, enabled).apply()
    }

    private companion object {
        const val PREFS = "pos-local-prefs"
        const val KEY_KITCHEN_TICKET_PRINT_FILTER = "kitchen_ticket_print_filter"
        const val KEY_SALES_CARD_NAMES_UPPERCASE = "sales_card_names_uppercase"
    }
}
