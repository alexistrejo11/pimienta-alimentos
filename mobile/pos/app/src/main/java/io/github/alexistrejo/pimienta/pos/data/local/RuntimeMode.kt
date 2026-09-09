package io.github.alexistrejo.pimienta.pos.data.local

import android.content.Context
import io.github.alexistrejo.pimienta.pos.BuildConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

enum class RuntimeMode { SANDBOX, PRODUCTION }

class RuntimeModeStore(context: Context) {
    private val prefs = context.getSharedPreferences("pos-runtime-mode", Context.MODE_PRIVATE)
    fun mode(): RuntimeMode {
        val value = prefs.getString("mode", null)
        return if (value == null) {
            if (BuildConfig.DEBUG) RuntimeMode.SANDBOX else RuntimeMode.PRODUCTION
        } else runCatching { RuntimeMode.valueOf(value) }.getOrDefault(RuntimeMode.PRODUCTION)
    }
    fun setMode(mode: RuntimeMode) { prefs.edit().putString("mode", mode.name).apply() }
}

class PosDatabaseProvider(private val context: Context) {
    val modes = RuntimeModeStore(context)
    private val lock = Any()
    private val migrationPrefs = context.getSharedPreferences("pos-database-migration", Context.MODE_PRIVATE)
    private var sandbox: PosDatabase? = null
    private var production: PosDatabase? = null

    init { migrateLegacyDatabase() }

    fun database(mode: RuntimeMode = modes.mode()): PosDatabase = synchronized(lock) {
        when (mode) {
            RuntimeMode.SANDBOX -> sandbox ?: PosDatabase.create(context, "pimienta-pos-sandbox.db").also { sandbox = it }
            RuntimeMode.PRODUCTION -> production ?: PosDatabase.create(context, "pimienta-pos-production.db").also { production = it }
        }
    }

    fun close(mode: RuntimeMode? = null) = synchronized(lock) {
        when (mode) {
            RuntimeMode.SANDBOX -> sandbox?.close().also { sandbox = null }
            RuntimeMode.PRODUCTION -> production?.close().also { production = null }
            null -> { sandbox?.close(); production?.close(); sandbox = null; production = null }
        }
    }

    private fun migrateLegacyDatabase() {
        val legacy = context.getDatabasePath("pimienta-pos.db")
        val target = context.getDatabasePath("pimienta-pos-sandbox.db")
        if (migrationPrefs.getBoolean("legacy-to-sandbox", false)) return
        if (!legacy.exists() || target.exists()) { migrationPrefs.edit().putBoolean("legacy-to-sandbox", true).apply(); return }
        move(legacy, target)
        move(File(legacy.path + "-wal"), File(target.path + "-wal"))
        move(File(legacy.path + "-shm"), File(target.path + "-shm"))
        if (target.exists()) migrationPrefs.edit().putBoolean("legacy-to-sandbox", true).apply()
    }

    private fun move(source: File, target: File) {
        if (!source.exists() || target.exists()) return
        runCatching { Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE) }
            .recoverCatching { Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    }
}
