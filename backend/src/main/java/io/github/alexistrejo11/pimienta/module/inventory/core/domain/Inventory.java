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
  /** Cantidad física disponible para venta/uso. May be negative after a POS sale. */
  private int availableQuantity;

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
    this.status = InventoryStatus.NORMAL;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
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

  public int getTotalQuantity() {
    return availableQuantity;
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

  /**
   * Sets on-hand quantity to a physical count. Does not reject a short location capacity:
   * POS sales may already have driven quantity negative and floored occupied capacity at zero.
   */
  public void adjust(int countedQuantity) {
    if (countedQuantity < 0) {
      throw new IllegalArgumentException("Counted quantity cannot be negative");
    }
    int delta = countedQuantity - this.availableQuantity;
    this.availableQuantity = countedQuantity;
    if (this.location != null && delta != 0) {
      this.location.adjustOccupiedSoft(delta);
    }
    recalculateStatus();
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Adds {@code signedDelta} to available quantity. Positive increases stock. Never rejects a
   * negative result or a short location capacity.
   */
  public void applyUnrestrictedDelta(int signedDelta) {
    if (signedDelta == 0) {
      return;
    }
    this.availableQuantity += signedDelta;
    if (this.location != null) {
      this.location.adjustOccupiedSoft(signedDelta);
    }
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
    applyUnrestrictedDelta(-quantityOut);
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

  public InventoryStatus getStatus() {
    return status;
  }

  public void setStatus(InventoryStatus status) {
    this.status = status;
  }
}