package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.CashWithdrawalEntity
import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// One withdrawal plus the people and queue states already stored on the tablet, for read-only display.
data class WithdrawalDetail(
    val withdrawal: CashWithdrawalEntity,
    val cashier: LocalUserEntity?,
    val authorizer: LocalUserEntity?,
    val printStatus: String?,
    val syncStatus: String?,
)

// Spanish labels for withdrawal history and tickets. Stored codes and statuses are never rewritten.
object WithdrawalLabels {
    private val timestampFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    fun reason(code: String): String = when (code) {
        "RESGUARDO_EFECTIVO" -> "Resguardo de efectivo"
        else -> code
    }

    // Falls back to the stored id when the user was removed from this tablet.
    fun operator(userId: String, user: LocalUserEntity?): String =
        user?.displayName?.takeIf { it.isNotBlank() } ?: "Operador #$userId"

    // Uses the role captured when the withdrawal was signed, not the user's current role.
    fun role(rawRole: String): String = LocalUserEntity.spanishRoleLabel(rawRole)

    fun timestamp(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(timestampFormatter)

    // Maps print_job.status values written by PosRepository and PrintJobProcessor.
    fun printStatus(status: String?): String = when (status) {
        "PRINTED" -> "Impreso"
        "PENDING", "PRINTING" -> "Pendiente"
        "FAILED" -> "Error"
        "EXPIRED" -> "Expirado"
        null -> "Sin registro"
        else -> status
    }

    // Maps outbox_event.status values written by PosRepository and SyncDao.
    fun syncStatus(status: String?): String = when (status) {
        "SYNCED" -> "Sincronizado"
        "PENDING", "IN_FLIGHT", "FAILED_RETRYABLE", "RETRY" -> "En cola"
        "REJECTED" -> "Rechazado"
        null -> "Sin registro"
        else -> status
    }
}
