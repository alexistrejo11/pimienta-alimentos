package io.github.alexistrejo.pimienta.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Stores a locally verifiable POS user snapshot, never a plaintext PIN.
@Entity(tableName = "local_user") data class LocalUserEntity(@PrimaryKey val id: String, val displayName: String, val role: String, val pinHash: String, val active: Boolean)
// Stores the stable debug device identity and its local event sequence.
@Entity(tableName = "device") data class DeviceEntity(@PrimaryKey val id: String, val name: String, val visibleCode: String, val nextEventSequence: Long, val siteId: String? = null, val status: String = "SANDBOX", val minAppVersion: String? = null, val schemaVersionsJson: String? = null)
// Stores the one active cash shift permitted for a device.
@Entity(tableName = "shift", indices = [Index(value = ["deviceId", "status"], unique = true)]) data class ShiftEntity(@PrimaryKey val id: String, val deviceId: String, val siteId: String, val cashierId: String, val openingCashCentavos: Long, val openedAtEpochMillis: Long, val status: String, val nextFolioNumber: Long)
// Stores a confirmed sale as an immutable local fact.
@Entity(tableName = "sale", indices = [Index(value = ["folio"], unique = true)]) data class SaleEntity(@PrimaryKey val id: String, val folio: String, val shiftId: String, val cashierId: String, val grossCentavos: Long, val discountCentavos: Long, val totalCentavos: Long, val paymentMethod: String, val tenderedCentavos: Long, val changeCentavos: Long, val confirmedAtEpochMillis: Long, val status: String = "CONFIRMED")
// Stores the single authorized discount attached to a confirmed sale.
@Entity(tableName = "sale_discount") data class SaleDiscountEntity(@PrimaryKey val id: String, val saleId: String, val amountCentavos: Long, val reason: String, val authorizedByUserId: String, val authorizedByRole: String, val authorizedAtEpochMillis: Long)
// Stores an immutable cash safeguard withdrawal made during an open shift.
@Entity(tableName = "cash_withdrawal", indices = [Index(value = ["folio"], unique = true)]) data class CashWithdrawalEntity(@PrimaryKey val id: String, val folio: String, val shiftId: String, val cashierId: String, val amountCentavos: Long, val reason: String, val authorizedByUserId: String, val authorizedByRole: String, val createdAtEpochMillis: Long)
// Stores the catalog information exactly as it was sold.
@Entity(tableName = "sale_line") data class SaleLineEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String?,
    val productName: String,
    val categoryName: String,
    val quantity: Int,
    val unitPriceCentavos: Long,
    val subtotalCentavos: Long,
    val stockPolicy: String,
    val lineType: String = "CATALOG",
    val sourceBarcode: String? = null,
    val authorizedByOperatorId: Long? = null,
    val authorizedAtEpochMillis: Long? = null,
)
// Stores the selected payment type and its applied amount.
@Entity(tableName = "payment") data class PaymentEntity(@PrimaryKey val id: String, val saleId: String, val method: String, val amountCentavos: Long)
// Records only controlled-stock deductions created by a sale.
@Entity(tableName = "inventory_movement") data class InventoryMovementEntity(@PrimaryKey val id: String, val saleId: String, val productId: String, val quantityDelta: Int, val createdAtEpochMillis: Long, val movementType: String = "SALE")
// Keeps a durable event until a future sync implementation delivers it.
@Entity(tableName = "outbox_event", indices = [Index(value = ["sequence"], unique = true)]) data class OutboxEventEntity(@PrimaryKey val id: String, val sequence: Long, val type: String, val aggregateId: String, val status: String, val createdAtEpochMillis: Long, val schemaVersion: Int = 1, val deviceId: String? = null, val siteId: String? = null, val shiftId: String? = null, val occurredAtEpochMillis: Long = createdAtEpochMillis, val payloadJson: String? = null, val attemptCount: Int = 0, val nextAttemptAtEpochMillis: Long = 0, val lastError: String? = null, val resultStatus: String? = null, val incidentId: String? = null) {
    // Reuses the immutable event id as the backend idempotency key.
    val idempotencyKey: String get() = id
}
@Entity(tableName = "sync_state") data class SyncStateEntity(@PrimaryKey val id: Int = 1, val baseUrl: String? = null, val changesCursor: String? = null, val bootstrapSnapshotId: String? = null, val lastSuccessfulAtEpochMillis: Long? = null, val lastError: String? = null, val status: String = "NOT_CONFIGURED")

// Keeps bounded diagnostic events until the next successful telemetry upload.
@Entity(tableName = "telemetry_event") data class TelemetryEventEntity(
    @PrimaryKey val id: String,
    val schemaVersion: Int = 1,
    val eventType: String,
    val level: String,
    val message: String,
    val stack: String? = null,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long = occurredAtEpochMillis,
)

// Keeps a durable print request and the latest delivery diagnostic.
@Entity(tableName = "print_job") data class PrintJobEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val status: String,
    val duplicate: Boolean,
    val createdAtEpochMillis: Long,
    val documentType: String = "SALE",
    val templateVersion: Int = 1,
    val attemptCount: Int = 0,
    val lastAttemptAtEpochMillis: Long? = null,
    val lastError: String? = null,
)

// Stores every blind count attempt before a manager seals the shift.
@Entity(tableName = "cash_count_attempt") data class CashCountAttemptEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val cashierId: String,
    val totalCentavos: Long,
    val denominations: String,
    val status: String,
    val note: String?,
    val createdAtEpochMillis: Long,
)

// Stores the immutable approved Corte Z snapshot.
@Entity(tableName = "shift_close") data class ShiftCloseEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val expectedCashCentavos: Long,
    val countedCashCentavos: Long,
    val differenceCentavos: Long,
    val approvedByUserId: String,
    val approvedAtEpochMillis: Long,
)

// Stores a permitted post-sale cash cancellation without deleting the sale.
@Entity(tableName = "sale_cancellation") data class SaleCancellationEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val shiftId: String,
    val reason: String,
    val authorizedByUserId: String,
    val authorizedByRole: String,
    val createdAtEpochMillis: Long,
)
