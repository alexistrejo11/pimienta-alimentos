package io.github.alexistrejo.pimienta.pos.data.local

// Defines the durable lifecycle of an offline sale or cash operation.
enum class PendingSyncStatus {
    PENDING,
    IN_FLIGHT,
    SYNCED,
    FAILED_RETRYABLE,
    REJECTED,
}

// Keeps the existing outbox table as the Phase 4 pending-sync queue.
typealias PendingSyncQueueEntity = io.github.alexistrejo.pimienta.pos.data.local.entity.OutboxEventEntity
