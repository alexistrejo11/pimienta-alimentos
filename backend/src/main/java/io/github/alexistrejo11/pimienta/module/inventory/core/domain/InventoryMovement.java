package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;

/**
 * Registro inmutable de UN movimiento de stock.
 * Nunca se actualiza ni borra — es el audit log del inventario.
 *
 * Un movimiento siempre va de/hacia una StorageLocation.
 * Para transferencias se generan DOS movimientos: uno OUT y uno IN.
 */
public class InventoryMovement extends BaseDomain<Long> {

  private Item item;
  private StorageLocation sourceLocation; // null en entradas puras (compra)
  private StorageLocation destinationLocation; // null en salidas puras (venta)

  // ─────────────────────────────────────────────
  // CANTIDADES Y VALOR
  // ─────────────────────────────────────────────
  private int quantity;
  /** Costo unitario al momento del movimiento (para trazabilidad de costos) */
  private BigDecimal unitCost;

  // ─────────────────────────────────────────────
  // CLASIFICACIÓN
  // ─────────────────────────────────────────────
  private InventoryMovementType type;

  /**
   * Dirección del movimiento respecto al inventario.
   * Útil para calcular saldos sin depender del tipo.
   */
  private MovementDirection direction;

  private String description;
  /** Referencia externa: número de OC, folio de venta, etc. */
  private String referenceNumber;

  // ─────────────────────────────────────────────
  // TRAZABILIDAD
  // ─────────────────────────────────────────────
  /** ID del empleado / usuario que ejecutó el movimiento */
  private Long performedById;
  /** Snapshot del stock disponible DESPUÉS de este movimiento */
  private int stockAfterMovement;

  /** Transacción contable que agrupa este movimiento (persistencia / consultas). */
  private Long transactionId;

  /** Set on OUT, and on an IN that reverses a POS sale. */
  private InventoryExitReason exitReason;

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum InventoryMovementType {
    IN,
    OUT,
    ADJUSTMENT_PLUS,
    ADJUSTMENT_MINUS
  }

  public enum MovementDirection {
    IN,
    OUT
  }

  // ─────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────

  public InventoryMovement() {
    this.id = 0L;
    this.quantity = 0;
    this.unitCost = BigDecimal.ZERO;
  }

  // ─────────────────────────────────────────────
  // FACTORY METHODS
  // ─────────────────────────────────────────────

  public static InventoryMovement recordIn(
      Item item, StorageLocation destination,
      int quantity, BigDecimal unitCost,
      InventoryExitReason exitReason, String referenceNumber,
      String description, Long performedById, int stockAfter) {

    return build(item, null, destination, quantity, unitCost,
        InventoryMovementType.IN, MovementDirection.IN, exitReason, referenceNumber,
        description, performedById, stockAfter);
  }

  public static InventoryMovement recordOut(
      Item item, StorageLocation source,
      int quantity, BigDecimal unitCost,
      InventoryExitReason exitReason, String referenceNumber,
      String description, Long performedById, int stockAfter) {

    return build(item, source, null, quantity, unitCost,
        InventoryMovementType.OUT, MovementDirection.OUT, exitReason, referenceNumber,
        description, performedById, stockAfter);
  }

  public static InventoryMovement recordAdjustment(
      Item item, StorageLocation location,
      int quantity, boolean increase, String referenceNumber,
      String description, Long performedById, int stockAfter) {

    if (increase) {
      return build(item, null, location, quantity, BigDecimal.ZERO,
          InventoryMovementType.ADJUSTMENT_PLUS, MovementDirection.IN, null, referenceNumber,
          description, performedById, stockAfter);
    }
    return build(item, location, null, quantity, BigDecimal.ZERO,
        InventoryMovementType.ADJUSTMENT_MINUS, MovementDirection.OUT, null, referenceNumber,
        description, performedById, stockAfter);
  }

  // ─────────────────────────────────────────────
  // BUILDER PRIVADO
  // ─────────────────────────────────────────────

  private static InventoryMovement build(
      Item item, StorageLocation source, StorageLocation destination,
      int quantity, BigDecimal unitCost,
      InventoryMovementType type, MovementDirection direction,
      InventoryExitReason exitReason,
      String referenceNumber, String description,
      Long performedById, int stockAfter) {

    var now = LocalDateTime.now();
    var m = new InventoryMovement();
    m.item = item;
    m.sourceLocation = source;
    m.destinationLocation = destination;
    m.quantity = quantity;
    m.unitCost = unitCost;
    m.type = type;
    m.direction = direction;
    m.exitReason = exitReason;
    m.referenceNumber = referenceNumber;
    m.description = description;
    m.performedById = performedById;
    m.stockAfterMovement = stockAfter;
    m.createdAt = now;
    m.updatedAt = now;
    return m;
  }

  // ─────────────────────────────────────────────
  // LÓGICA DE DOMINIO
  // ─────────────────────────────────────────────

  /** Valor total del movimiento (cantidad × costo unitario) */
  public BigDecimal getTotalValue() {
    return unitCost.multiply(new BigDecimal(quantity));
  }

  // ─────────────────────────────────────────────
  // GETTERS (sin setters — movimiento es inmutable)
  // ─────────────────────────────────────────────

  public Item getItem() {
    return item;
  }

  public StorageLocation getSourceLocation() {
    return sourceLocation;
  }

  public StorageLocation getDestinationLocation() {
    return destinationLocation;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public InventoryMovementType getType() {
    return type;
  }

  public MovementDirection getDirection() {
    return direction;
  }

  public String getDescription() {
    return description;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public Long getPerformedById() {
    return performedById;
  }

  public int getStockAfterMovement() {
    return stockAfterMovement;
  }

  public InventoryExitReason getExitReason() {
    return exitReason;
  }

  public void setExitReason(InventoryExitReason exitReason) {
    this.exitReason = exitReason;
  }

  public Long getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public void setSourceLocation(StorageLocation sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public void setDestinationLocation(StorageLocation destinationLocation) {
    this.destinationLocation = destinationLocation;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public void setType(InventoryMovementType type) {
    this.type = type;
  }

  public void setDirection(MovementDirection direction) {
    this.direction = direction;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public void setPerformedById(Long performedById) {
    this.performedById = performedById;
  }

  public void setStockAfterMovement(int stockAfterMovement) {
    this.stockAfterMovement = stockAfterMovement;
  }
}