package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.time.LocalDateTime;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;

/**
 * Inventory = registro de CUÁNTO hay de un Item en UNA StorageLocation.
 *
 * Relación: Item 1 ──── N Inventory N ──── 1 StorageLocation
 *
 * Si el mismo ítem está en tres bodegas distintas, existirán tres registros
 * Inventory, uno por cada (item, location).
 */
public class Inventory extends BaseDomain<Long> {

  private Item item;
  private StorageLocation location;

  // ─────────────────────────────────────────────
  // CANTIDADES
  // ─────────────────────────────────────────────
  /** Cantidad física disponible para venta/uso */
  private int availableQuantity;
  /** Reservado por órdenes pendientes (no disponible aún para otros) */
  private int reservedQuantity;
  /** En tránsito / recepción pendiente */
  private int inTransitQuantity;

  private InventoryStatus status;

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum InventoryStatus {
    NORMAL,
    LOW_STOCK, // availableQuantity <= item.reorderPoint
    OUT_OF_STOCK,
    OVERSTOCKED
  }

  // ─────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────

  public Inventory() {
    this.id = 0L;
    this.availableQuantity = 0;
    this.reservedQuantity = 0;
    this.inTransitQuantity = 0;
    this.status = InventoryStatus.NORMAL;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  // ─────────────────────────────────────────────
  // FACTORY METHODS
  // ─────────────────────────────────────────────

  public static Inventory create(Item item, StorageLocation location, int initialQuantity) {
    var now = LocalDateTime.now();
    var inv = new Inventory();
    inv.item = item;
    inv.location = location;
    inv.availableQuantity = initialQuantity;
    inv.createdAt = now;
    inv.updatedAt = now;
    if (initialQuantity > 0) {
      location.addStock(initialQuantity);
    }
    inv.recalculateStatus();
    return inv;
  }

  // ─────────────────────────────────────────────
  // LÓGICA DE DOMINIO
  // ─────────────────────────────────────────────

  /** Cantidad total física (disponible + reservado) */
  public int getTotalQuantity() {
    return availableQuantity + reservedQuantity;
  }

  /** Entrada de stock (compra, devolución, ajuste +) */
  public void addStock(int quantity) {
    if (quantity <= 0)
      throw new IllegalArgumentException("La cantidad debe ser positiva");
    this.availableQuantity += quantity;
    this.location.addStock(quantity);
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /** Salida de stock (venta, uso, ajuste -) */
  public void removeStock(int quantity) {
    if (quantity <= 0)
      throw new IllegalArgumentException("La cantidad debe ser positiva");
    if (quantity > this.availableQuantity)
      throw new IllegalStateException("Stock insuficiente: disponible=" + availableQuantity);
    this.availableQuantity -= quantity;
    this.location.removeStock(quantity);
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /** Reserva stock para una orden (lo separa pero no lo egresa aún) */
  public void reserve(int quantity) {
    if (quantity > this.availableQuantity)
      throw new IllegalStateException("No hay suficiente stock para reservar");
    this.availableQuantity -= quantity;
    this.reservedQuantity += quantity;
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /** Confirma salida de stock previamente reservado */
  public void confirmReservation(int quantity) {
    if (quantity > this.reservedQuantity)
      throw new IllegalStateException("No hay suficiente stock reservado");
    this.reservedQuantity -= quantity;
    this.location.removeStock(quantity);
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /** Cancela una reserva y regresa al disponible */
  public void cancelReservation(int quantity) {
    if (quantity > this.reservedQuantity)
      throw new IllegalStateException("No hay suficiente stock reservado para cancelar");
    this.reservedQuantity -= quantity;
    this.availableQuantity += quantity;
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /** Ajuste directo (inventario físico) — puede subir o bajar */
  public void adjust(int newAvailableQuantity) {
    int delta = newAvailableQuantity - this.availableQuantity;
    this.availableQuantity = newAvailableQuantity;
    if (delta > 0)
      this.location.addStock(delta);
    else if (delta < 0)
      this.location.removeStock(Math.abs(delta));
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  public boolean needsReorder() {
    return item != null && availableQuantity <= item.getReorderPoint();
  }

  /**
   * Apply a POS sale (or inverse) quantity change. Decreases stock when {@code quantityOut} &gt; 0.
   * Allows negative available quantity. Only valid for {@link StorageLocation.LocationType#POS}.
   */
  public void applyPosSaleDelta(int quantityOut) {
    if (quantityOut == 0) {
      return;
    }
    if (location == null || !location.isPos()) {
      throw new IllegalStateException("POS stock delta is only allowed on LocationType.POS");
    }
    this.availableQuantity -= quantityOut;
    location.adjustOccupiedSoft(-quantityOut);
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  private void recalculateStatus() {
    if (availableQuantity <= 0) {
      this.status = InventoryStatus.OUT_OF_STOCK;
    } else if (item != null && availableQuantity <= item.getReorderPoint()) {
      this.status = InventoryStatus.LOW_STOCK;
    } else {
      this.status = InventoryStatus.NORMAL;
    }
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
  }

  // ─────────────────────────────────────────────
  // GETTERS & SETTERS
  // ─────────────────────────────────────────────

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public StorageLocation getLocation() {
    return location;
  }

  public void setLocation(StorageLocation location) {
    this.location = location;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(int availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }

  public void setReservedQuantity(int reservedQuantity) {
    this.reservedQuantity = reservedQuantity;
  }

  public int getInTransitQuantity() {
    return inTransitQuantity;
  }

  public void setInTransitQuantity(int inTransitQuantity) {
    this.inTransitQuantity = inTransitQuantity;
  }

  public InventoryStatus getStatus() {
    return status;
  }

  public void setStatus(InventoryStatus status) {
    this.status = status;
  }
}