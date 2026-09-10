package io.github.alexistrejo.pimienta.pos.data.telemetry

import io.github.alexistrejo.pimienta.pos.data.local.PosDatabase
import io.github.alexistrejo.pimienta.pos.data.local.entity.TelemetryEventEntity
import java.util.UUID

private const val MAX_TELEMETRY_EVENTS = 200

// Stores safe, bounded diagnostics in the active Room database.
class PosTelemetryLogger(private val database: PosDatabase) {
    // Adds one diagnostic event and removes the oldest excess entries.
    fun record(level: String, eventType: String, message: String, stack: String? = null) {
        val now = System.currentTimeMillis()
        database.operationsDao().insertTelemetry(
            TelemetryEventEntity(
                id = UUID.randomUUID().toString(),
                eventType = clean(eventType, 40),
                level = normalizeLevel(level),
                message = clean(message, 2000),
                stack = stack?.let { clean(it, 12000) },
                occurredAtEpochMillis = now,
                createdAtEpochMillis = now,
            ),
        )
        database.operationsDao().trimTelemetry(MAX_TELEMETRY_EVENTS)
    }

    private fun normalizeLevel(level: String): String = when (level.uppercase()) {
        "ERROR", "WARN", "INFO" -> level.uppercase()
        else -> "INFO"
    }

    private fun clean(value: String, max: Int): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return singleLine.take(max)
    }
}
