package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;

/**
 * Una transacción agrupa uno o varios InventoryMovement bajo un mismo folio.
 *
 * Ejemplo: una orden de compra genera una transacción con múltiples
 * movimientos (uno por cada ítem recibido).
 *
 * Jerarquía: InventoryTransaction 1 ──── N InventoryMovement
 */
public class InventoryTransaction extends BaseDomain<Long> {

  /** Folio / número de transacción (único, human-readable) */
  private String transactionNumber;

  private TransactionType type;
  private TransactionStatus status;

  /** Referencia externa: OC, factura, folio de venta */
  private String externalReference;
  private String notes;

  /** Quién inició la transacción */
  private Long initiatedById;
  /** Quién la aprobó (si aplica) */
  private Long approvedById;

  private LocalDateTime approvedAt;
  private LocalDateTime completedAt;

  /** Movimientos que forman esta transacción */
  private List<InventoryMovement> movements = new ArrayList<>();

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum TransactionType {
    PURCHASE_RECEIPT, // Recepción de compra
    SALE_DISPATCH, // Despacho por venta
    INTERNAL_TRANSFER, // Transferencia entre ubicaciones
    PHYSICAL_COUNT, // Conteo físico aprobado
    ADJUSTMENT, // Ajuste directo autorizado (ADMIN)
    RETURN_FROM_CLIENT, // Devolución recibida de cliente
    RETURN_TO_SUPPLIER, // Devolución enviada a proveedor
    PRODUCTION_ISSUE, // Salida para producción
    SCRAP_WRITE_OFF // Baja por merma o daño
  }

  public enum TransactionStatus {
    DRAFT, // Creada, aún editable
    PENDING, // Esperando aprobación
    APPROVED, // Aprobada, lista para ejecutar
    IN_PROGRESS, // Ejecutándose (movimientos en curso)
    COMPLETED, // Todos los movimientos confirmados
    CANCELLED // Anulada
  }

  // ─────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────

  public InventoryTransaction() {
    this.id = 0L;
    this.status = TransactionStatus.DRAFT;
    this.movements = new ArrayList<>();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  // ─────────────────────────────────────────────
  // FACTORY METHOD
  // ─────────────────────────────────────────────

  public static InventoryTransaction open(String transactionNumber, TransactionType type,
      String externalReference, String notes,
      Long initiatedById) {
    var tx = new InventoryTransaction();
    tx.transactionNumber = transactionNumber;
    tx.type = type;
    tx.externalReference = externalReference;
    tx.notes = notes;
    tx.initiatedById = initiatedById;
    tx.status = TransactionStatus.DRAFT;
    tx.createdAt = LocalDateTime.now();
    tx.updatedAt = LocalDateTime.now();
    return tx;
  }

  // ─────────────────────────────────────────────
  // LÓGICA DE DOMINIO
  // ─────────────────────────────────────────────

  public void addMovement(InventoryMovement movement) {
    if (status != TransactionStatus.DRAFT && status != TransactionStatus.APPROVED)
      throw new IllegalStateException("No se pueden agregar movimientos en estado " + status);
    this.movements.add(movement);
    this.updatedAt = LocalDateTime.now();
  }

  public void submit() {
    if (movements.isEmpty())
      throw new IllegalStateException("La transacción no tiene movimientos");
    this.status = TransactionStatus.PENDING;
    this.updatedAt = LocalDateTime.now();
  }

  public void approve(Long approvedById) {
    if (status != TransactionStatus.PENDING)
      throw new IllegalStateException("Solo se pueden aprobar transacciones PENDING");
    this.status = TransactionStatus.APPROVED;
    this.approvedById = approvedById;
    this.approvedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void complete() {
    this.status = TransactionStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public void cancel() {
    if (status == TransactionStatus.COMPLETED)
      throw new IllegalStateException("No se puede cancelar una transacción completada");
    this.status = TransactionStatus.CANCELLED;
    this.updatedAt = LocalDateTime.now();
  }

  /** Valor total de todos los movimientos de esta transacción */
  public BigDecimal getTotalValue() {
    return movements.stream()
        .map(InventoryMovement::getTotalValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public int getTotalUnits() {
    return movements.stream().mapToInt(InventoryMovement::getQuantity).sum();
  }

  // ─────────────────────────────────────────────
  // GETTERS & SETTERS
  // ─────────────────────────────────────────────

  public String getTransactionNumber() {
    return transactionNumber;
  }

  public void setTransactionNumber(String transactionNumber) {
    this.transactionNumber = transactionNumber;
  }

  public TransactionType getType() {
    return type;
  }

  public void setType(TransactionType type) {
    this.type = type;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Long getInitiatedById() {
    return initiatedById;
  }

  public void setInitiatedById(Long initiatedById) {
    this.initiatedById = initiatedById;
  }

  public Long getApprovedById() {
    return approvedById;
  }

  public LocalDateTime getApprovedAt() {
    return approvedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  /** Vista inmutable de los movimientos */
  public List<InventoryMovement> getMovements() {
    return Collections.unmodifiableList(movements);
  }

  /** Reemplaza movimientos al cargar desde persistencia (no usar en reglas de negocio). */
  public void replaceMovementsForLoad(List<InventoryMovement> loaded) {
    this.movements = new ArrayList<>(loaded);
  }

  public void setStatus(TransactionStatus status) {
    this.status = status;
  }

  public void setApprovedById(Long approvedById) {
    this.approvedById = approvedById;
  }

  public void setApprovedAt(LocalDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}