package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.time.LocalDateTime;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;

/**
 * Representa un lugar físico donde se almacenan ítems.
 * Soporta jerarquía: Almacén → Zona → Pasillo → Estante → Nivel.
 * Un Inventory puede tener uno o varios StorageLocation.
 */
public class StorageLocation extends BaseDomain<Long> {

  // ─────────────────────────────────────────────
  // IDENTIFICACIÓN
  // ─────────────────────────────────────────────
  /** Código legible: ej. "ALM-A1-P3-E2" */
  private String code;
  private String name;
  private String description;

  // ─────────────────────────────────────────────
  // JERARQUÍA
  // ─────────────────────────────────────────────
  private LocationType type;

  /**
   * ID del location padre en la jerarquía.
   * null = nivel raíz (el almacén / edificio).
   */
  private Long parentId;

  // ─────────────────────────────────────────────
  // CAPACIDAD
  // ─────────────────────────────────────────────
  /** Capacidad máxima en unidades lógicas */
  private int maxCapacity;
  /** Unidades ocupadas actualmente (se actualiza vía dominio) */
  private int occupiedCapacity;

  private LocationStatus status;

  /**
   * Headquarter that owns this location when {@link LocationType#POS}; null for warehouse tree
   * locations.
   */
  private Long headquarterId;

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum LocationType {
    WAREHOUSE, // Almacén completo
    ZONE, // Zona dentro del almacén (ej. "Refrigerados")
    AISLE, // Pasillo
    SHELF, // Estante
    BIN, // Casillero / ubicación hoja
    POS // Canonical cafeteria POS stock location for a headquarter
  }

  public enum LocationStatus {
    ACTIVE,
    FULL,
    BLOCKED, // Bloqueado por mantenimiento o cuarentena
    INACTIVE
  }

  // ─────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────

  public StorageLocation() {
    this.id = 0L;
    this.code = "";
    this.name = "";
    this.maxCapacity = 0;
    this.occupiedCapacity = 0;
    this.status = LocationStatus.ACTIVE;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  // ─────────────────────────────────────────────
  // FACTORY METHODS
  // ─────────────────────────────────────────────

  public static StorageLocation create(String code, String name, String description,
      LocationType type, Long parentId, int maxCapacity) {
    return create(code, name, description, type, parentId, maxCapacity, null);
  }

  public static StorageLocation create(String code, String name, String description,
      LocationType type, Long parentId, int maxCapacity, Long headquarterId) {
    var now = LocalDateTime.now();
    var loc = new StorageLocation();
    loc.code = code;
    loc.name = name;
    loc.description = description;
    loc.type = type;
    loc.parentId = parentId;
    loc.maxCapacity = maxCapacity;
    loc.occupiedCapacity = 0;
    loc.headquarterId = headquarterId;
    loc.status = LocationStatus.ACTIVE;
    loc.createdAt = now;
    loc.updatedAt = now;
    return loc;
  }

  /** Stable code for the canonical POS location of a headquarter. */
  public static String posCodeFor(long headquarterId) {
    return "POS-" + headquarterId;
  }

  public boolean isPos() {
    return type == LocationType.POS;
  }

  /**
   * Soft capacity update for POS (no hard fail on over/under). Warehouse locations keep
   * {@link #addStock(int)} / {@link #removeStock(int)}.
   */
  public void adjustOccupiedSoft(int delta) {
    if (delta == 0) {
      return;
    }
    this.occupiedCapacity = Math.max(0, this.occupiedCapacity + delta);
    if (this.status == LocationStatus.FULL && this.occupiedCapacity < this.maxCapacity) {
      this.status = LocationStatus.ACTIVE;
    }
    this.updatedAt = LocalDateTime.now();
  }

  // ─────────────────────────────────────────────
  // LÓGICA DE DOMINIO
  // ─────────────────────────────────────────────

  public int getAvailableCapacity() {
    return maxCapacity - occupiedCapacity;
  }

  public boolean hasCapacityFor(int units) {
    return getAvailableCapacity() >= units;
  }

  /** Registra entrada de unidades y actualiza estado automáticamente */
  public void addStock(int units) {
    if (units <= 0)
      throw new IllegalArgumentException("Las unidades deben ser positivas");
    if (!hasCapacityFor(units))
      throw new IllegalStateException("Capacidad insuficiente en ubicación " + code);
    this.occupiedCapacity += units;
    this.status = (this.occupiedCapacity >= this.maxCapacity)
        ? LocationStatus.FULL
        : LocationStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  /** Registra salida de unidades */
  public void removeStock(int units) {
    if (units <= 0)
      throw new IllegalArgumentException("Las unidades deben ser positivas");
    if (units > this.occupiedCapacity)
      throw new IllegalStateException("No hay suficiente stock en " + code);
    this.occupiedCapacity -= units;
    if (this.status == LocationStatus.FULL)
      this.status = LocationStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void block() {
    this.status = LocationStatus.BLOCKED;
    this.updatedAt = LocalDateTime.now();
  }

  public void unblock() {
    this.status = LocationStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // ─────────────────────────────────────────────
  // GETTERS & SETTERS
  // ─────────────────────────────────────────────

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocationType getType() {
    return type;
  }

  public void setType(LocationType type) {
    this.type = type;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public int getMaxCapacity() {
    return maxCapacity;
  }

  public void setMaxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  public int getOccupiedCapacity() {
    return occupiedCapacity;
  }

  public void setOccupiedCapacity(int occupiedCapacity) {
    this.occupiedCapacity = occupiedCapacity;
  }

  public LocationStatus getStatus() {
    return status;
  }

  public void setStatus(LocationStatus status) {
    this.status = status;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }
}
