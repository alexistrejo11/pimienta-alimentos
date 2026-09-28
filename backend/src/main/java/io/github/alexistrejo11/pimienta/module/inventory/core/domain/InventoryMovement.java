package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum InventoryMovementType {
    // Entradas
    PURCHASE, // Compra a proveedor
    RETURN_FROM_CLIENT, // Devolución de cliente
    INITIAL_STOCK, // Carga inicial del sistema
    // Salidas
    SALE, // Venta
    RETURN_TO_SUPPLIER, // Devolución a proveedor
    USAGE, // Consumo interno (producción, etc.)
    SCRAP, // Merma / baja definitiva
    // Bidireccional
    TRANSFER, // Transferencia entre ubicaciones
    ADJUSTMENT_PLUS, // Ajuste físico positivo
    ADJUSTMENT_MINUS // Ajuste físico negativo
  }

  public enum MovementDirection {
    IN, // Incrementa el inventario disponible
    OUT, // Decrementa el inventario disponible
    NEUTRAL // Sin impacto en total (solo cambia ubicación)
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

  public static InventoryMovement recordEntry(
      Item item, StorageLocation destination,
      int quantity, BigDecimal unitCost,
      InventoryMovementType type, String referenceNumber,
      String description, Long performedById, int stockAfter) {

    return build(item, null, destination, quantity, unitCost,
        type, MovementDirection.IN, referenceNumber,
        description, performedById, stockAfter);
  }

  public static InventoryMovement recordExit(
      Item item, StorageLocation source,
      int quantity, BigDecimal unitCost,
      InventoryMovementType type, String referenceNumber,
      String description, Long performedById, int stockAfter) {

    return build(item, source, null, quantity, unitCost,
        type, MovementDirection.OUT, referenceNumber,
        description, performedById, stockAfter);
  }

  public static InventoryMovement recordTransfer(
      Item item, StorageLocation source, StorageLocation destination,
      int quantity, BigDecimal unitCost,
      String referenceNumber, String description,
      Long performedById, int stockAfter) {

    return build(item, source, destination, quantity, unitCost,
        InventoryMovementType.TRANSFER, MovementDirection.NEUTRAL,
        referenceNumber, description, performedById, stockAfter);
  }

  // ─────────────────────────────────────────────
  // BUILDER PRIVADO
  // ─────────────────────────────────────────────

  private static InventoryMovement build(
      Item item, StorageLocation source, StorageLocation destination,
      int quantity, BigDecimal unitCost,
      InventoryMovementType type, MovementDirection direction,
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