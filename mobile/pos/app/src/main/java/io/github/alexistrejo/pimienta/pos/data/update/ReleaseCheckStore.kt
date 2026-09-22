package io.github.alexistrejo.pimienta.pos.data.update

import android.content.Context
import android.content.SharedPreferences

/** Tiny key-value surface so release-check cache can be unit-tested without Robolectric. */
interface ReleaseCheckStore {
    fun getLong(key: String, default: Long): Long

    fun getInt(key: String, default: Int): Int

    fun getString(key: String): String?

    fun edit(block: ReleaseCheckEditor.() -> Unit)
}

interface ReleaseCheckEditor {
    fun putLong(key: String, value: Long): ReleaseCheckEditor

    fun putInt(key: String, value: Int): ReleaseCheckEditor

    fun putString(key: String, value: String): ReleaseCheckEditor

    fun remove(key: String): ReleaseCheckEditor

    fun apply()
}

class PrefsReleaseCheckStore(private val prefs: SharedPreferences) : ReleaseCheckStore {
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun edit(block: ReleaseCheckEditor.() -> Unit) {
        PrefsReleaseCheckEditor(prefs.edit()).also(block)
    }
}

private class PrefsReleaseCheckEditor(
    private val editor: SharedPreferences.Editor,
) : ReleaseCheckEditor {
    override fun putLong(key: String, value: Long) = apply { editor.putLong(key, value) }

    override fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }

    override fun putString(key: String, value: String) = apply { editor.putString(key, value) }

    override fun remove(key: String) = apply { editor.remove(key) }

    override fun apply() {
        editor.apply()
    }
}

/** In-memory store for JVM unit tests. */
class MemoryReleaseCheckStore : ReleaseCheckStore {
    private val longs = mutableMapOf<String, Long>()
    private val ints = mutableMapOf<String, Int>()
    private val strings = mutableMapOf<String, String>()

    override fun getLong(key: String, default: Long): Long = longs[key] ?: default

    override fun getInt(key: String, default: Int): Int = ints[key] ?: default

    override fun getString(key: String): String? = strings[key]

    override fun edit(block: ReleaseCheckEditor.() -> Unit) {
        MemoryReleaseCheckEditor().also(block)
    }

    private inner class MemoryReleaseCheckEditor : ReleaseCheckEditor {
        override fun putLong(key: String, value: Long) = apply { longs[key] = value }

        override fun putInt(key: String, value: Int) = apply { ints[key] = value }

        override fun putString(key: String, value: String) = apply { strings[key] = value }

        override fun remove(key: String) =
            apply {
                longs.remove(key)
                ints.remove(key)
                strings.remove(key)
            }

        override fun apply() = Unit
    }
}
