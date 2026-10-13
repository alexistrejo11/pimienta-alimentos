package io.github.alexistrejo.pimienta.pos.domain

import io.github.alexistrejo.pimienta.pos.data.local.entity.LocalUserEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

// Verifies withdrawal audit labels shown in the history panel and on the printed ticket.
class WithdrawalLabelsTest {
    private val laura = LocalUserEntity("5", "Laura", "MANAGER", "hash", true)

    @Test fun reason_code_is_shown_in_spanish_and_unknown_codes_pass_through() {
        assertEquals("Resguardo de efectivo", WithdrawalLabels.reason("RESGUARDO_EFECTIVO"))
        assertEquals("OTRO_MOTIVO", WithdrawalLabels.reason("OTRO_MOTIVO"))
    }

    @Test fun operator_uses_local_name_or_falls_back_to_id() {
        assertEquals("Laura", WithdrawalLabels.operator("5", laura))
        assertEquals("Operador #2", WithdrawalLabels.operator("2", null))
        assertEquals("Operador #7", WithdrawalLabels.operator("7", laura.copy(displayName = " ")))
    }

    @Test fun role_uses_existing_spanish_role_labels() {
        assertEquals("Gerente", WithdrawalLabels.role("MANAGER"))
        assertEquals("Administrador", WithdrawalLabels.role("SUPERADMIN"))
        assertEquals("Cajero", WithdrawalLabels.role("cashier"))
    }

    @Test fun timestamp_uses_ticket_date_convention() {
        val zone = ZoneId.of("America/Mexico_City")
        val millis = LocalDateTime.of(2026, 10, 12, 14, 32).atZone(zone).toInstant().toEpochMilli()
        assertEquals("12/10/2026 14:32", WithdrawalLabels.timestamp(millis, zone))
    }

    @Test fun print_status_maps_existing_print_job_states() {
        assertEquals("Impreso", WithdrawalLabels.printStatus("PRINTED"))
        assertEquals("Pendiente", WithdrawalLabels.printStatus("PENDING"))
        assertEquals("Pendiente", WithdrawalLabels.printStatus("PRINTING"))
        assertEquals("Error", WithdrawalLabels.printStatus("FAILED"))
        assertEquals("Expirado", WithdrawalLabels.printStatus("EXPIRED"))
        assertEquals("Sin registro", WithdrawalLabels.printStatus(null))
    }

    @Test fun sync_status_maps_existing_outbox_states() {
        assertEquals("Sincronizado", WithdrawalLabels.syncStatus("SYNCED"))
        listOf("PENDING", "IN_FLIGHT", "FAILED_RETRYABLE", "RETRY").forEach {
            assertEquals("En cola", WithdrawalLabels.syncStatus(it))
        }
        assertEquals("Rechazado", WithdrawalLabels.syncStatus("REJECTED"))
        assertEquals("Sin registro", WithdrawalLabels.syncStatus(null))
    }
}
