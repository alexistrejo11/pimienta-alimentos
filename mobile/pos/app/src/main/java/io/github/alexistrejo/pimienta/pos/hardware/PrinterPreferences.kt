package io.github.alexistrejo.pimienta.pos.hardware

import android.content.Context

// Remembers the paired thermal printer. Stays outside Room so training resets do not forget it.
class PrinterPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mac(): String? = prefs.getString(KEY_MAC, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun saveMac(mac: String) {
        val next = mac.trim()
        val previous = mac()
        val editor = prefs.edit().putString(KEY_MAC, next)
        if (!previous.equals(next, ignoreCase = true)) {
            editor.remove(KEY_STRATEGY)
        }
        editor.apply()
    }

    fun clearMac() {
        prefs.edit().remove(KEY_MAC).remove(KEY_STRATEGY).apply()
    }

    fun rfcommStrategy(): RfcommStrategy? =
        prefs.getString(KEY_STRATEGY, null)?.let { runCatching { RfcommStrategy.valueOf(it) }.getOrNull() }

    fun saveRfcommStrategy(strategy: RfcommStrategy) {
        prefs.edit().putString(KEY_STRATEGY, strategy.name).apply()
    }

    private companion object {
        const val PREFS = "pos-printer"
        const val KEY_MAC = "bluetooth_mac"
        const val KEY_STRATEGY = "rfcomm_strategy"
    }
}
