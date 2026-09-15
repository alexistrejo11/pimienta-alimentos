package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;

public class Item extends BaseDomain<Long> {

  // ─────────────────────────────────────────────
  // IDENTIFICACIÓN
  // ─────────────────────────────────────────────
  /** Código interno / SKU */
  private String sku;
  private String name;
  private String description;

  // ─────────────────────────────────────────────
  // CLASIFICACIÓN
  // ─────────────────────────────────────────────
  private ItemCategory category;
  private ItemUnit unit; // pieza, kg, litro, caja…
  private String brand;
  private String barcode;

  // ─────────────────────────────────────────────
  // COSTO
  // ─────────────────────────────────────────────
  /** Precio de compra (costo). Sale price lives on headquarter_items. */
  private BigDecimal costPrice;

  // ─────────────────────────────────────────────
  // STOCK — quantity se elimina de aquí:
  // la cantidad real vive en Inventory, no en Item.
  // Item describe QUÉ es; Inventory describe CUÁNTO hay y DÓNDE.
  // ─────────────────────────────────────────────
  /** Cantidad mínima antes de disparar alerta de reorden */
  private int reorderPoint;
  /** Cantidad sugerida al reordenar */
  private int reorderQuantity;

  private ItemStatus status;
  private CatalogRole catalogRole = CatalogRole.INVENTORY_ONLY;

  // ─────────────────────────────────────────────
  // ENUMERACIONES
  // ─────────────────────────────────────────────

  public enum ItemCategory {
    RAW_MATERIAL, // materia prima
    FINISHED_GOOD, // producto terminado
    CONSUMABLE, // consumible (papel, limpieza…)
    SPARE_PART, // refacción
    PACKAGING, // empaque
    TOOL, // herramienta
    MACHINE, // máquina
    FURNITURE, // mobiliario
    OTHER, // otro
  }

  public enum ItemUnit {
    PIECE,
    KG,
    GRAM,
    LITER,
    ML,
    BOX,
    DOZEN,
    METER,
    SQUARE_METER
  }

  public enum ItemStatus {
    ACTIVE,
    DISCONTINUED,
    OUT_OF_STOCK,
    PENDING_APPROVAL
  }

  public enum CatalogRole { INVENTORY_ONLY, POS_SELLABLE }

  // ─────────────────────────────────────────────
  // CONSTRUCTOR
  // ─────────────────────────────────────────────

  public Item() {
    this.id = 0L;
    this.name = "";
    this.description = "";
    this.sku = "";
    this.costPrice = BigDecimal.ZERO;
    this.reorderPoint = 0;
    this.reorderQuantity = 0;
    this.unit = ItemUnit.PIECE;
    this.status = ItemStatus.ACTIVE;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  // ─────────────────────────────────────────────
  // FACTORY METHODS
  // ─────────────────────────────────────────────

  public static Item create(String sku, String name, String description,
      BigDecimal costPrice,
      ItemCategory category, ItemUnit unit,
      int reorderPoint, int reorderQuantity) {
    var now = LocalDateTime.now();
    var item = new Item();
    item.sku = sku;
    item.name = name;
    item.description = description;
    item.costPrice = costPrice;
    item.category = category;
    item.unit = unit;
    item.reorderPoint = reorderPoint;
    item.reorderQuantity = reorderQuantity;
    item.status = ItemStatus.ACTIVE;
    item.createdAt = now;
    item.updatedAt = now;
    return item;
  }

  public static Item update(Long id, String sku, String name, String description,
      BigDecimal costPrice,
      ItemCategory category, ItemUnit unit,
      int reorderPoint, int reorderQuantity) {
    var now = LocalDateTime.now();
    var item = new Item();
    item.id = id;
    item.sku = sku;
    item.name = name;
    item.description = description;
    item.costPrice = costPrice;
    item.category = category;
    item.unit = unit;
    item.reorderPoint = reorderPoint;
    item.reorderQuantity = reorderQuantity;
    item.status = ItemStatus.ACTIVE;
    item.updatedAt = now;
    return item;
  }

  // ─────────────────────────────────────────────
  // LÓGICA DE DOMINIO
  // ─────────────────────────────────────────────

  public void discontinue() {
    this.status = ItemStatus.DISCONTINUED;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.status = ItemStatus.ACTIVE;
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // ─────────────────────────────────────────────
  // GETTERS & SETTERS
  // ─────────────────────────────────────────────

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
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

  public ItemCategory getCategory() {
    return category;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

  public ItemUnit getUnit() {
    return unit;
  }

  public void setUnit(ItemUnit unit) {
    this.unit = unit;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public CatalogRole getCatalogRole() { return catalogRole; }
  public void setCatalogRole(CatalogRole catalogRole) { this.catalogRole = catalogRole != null ? catalogRole : CatalogRole.INVENTORY_ONLY; }

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }

  public int getReorderPoint() {
    return reorderPoint;
  }

  public void setReorderPoint(int reorderPoint) {
    this.reorderPoint = reorderPoint;
  }

  public int getReorderQuantity() {
    return reorderQuantity;
  }

  public void setReorderQuantity(int reorderQuantity) {
    this.reorderQuantity = reorderQuantity;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public void setStatus(ItemStatus status) {
    this.status = status;
  }
}
