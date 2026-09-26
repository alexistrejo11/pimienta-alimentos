package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context

// Remembers the paired thermal printer. Stays outside Room so training resets do not forget it.
class PrinterPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mac(): String? = prefs.getString(KEY_MAC, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun saveMac(mac: String) {
        prefs.edit().putString(KEY_MAC, mac.trim()).apply()
    }

    fun clearMac() {
        prefs.edit().remove(KEY_MAC).apply()
    }

    private companion object {
        const val PREFS = "pos-printer"
        const val KEY_MAC = "bluetooth_mac"
    }
}
