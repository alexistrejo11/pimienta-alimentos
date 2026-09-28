package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Warehouse thing that can be counted. Sale identity lives on {@code products}. */
public class Item extends BaseDomain<Long> {

  private String name;
  private String description;
  private ItemCategory category;
  private ItemUnit unit;
  private String brand;
  private BigDecimal costPrice;
  private int reorderPoint;
  private int reorderQuantity;
  private ItemStatus status;

  /** Sale SKU when a product points at this item. Not stored on the item row. */
  private String saleSku;

  /** Product that points at this item. Not stored on the item row. */
  private Long productId;

  public enum ItemKind {
    PRODUCT,
    WAREHOUSE
  }

  public enum ItemCategory {
    RAW_MATERIAL,
    FINISHED_GOOD,
    CONSUMABLE,
    SPARE_PART,
    PACKAGING,
    TOOL,
    MACHINE,
    FURNITURE,
    OTHER
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

  public Item() {
    this.id = 0L;
    this.name = "";
    this.description = "";
    this.costPrice = BigDecimal.ZERO;
    this.reorderPoint = 0;
    this.reorderQuantity = 0;
    this.unit = ItemUnit.PIECE;
    this.status = ItemStatus.ACTIVE;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static Item create(
      String name,
      String description,
      BigDecimal costPrice,
      ItemCategory category,
      ItemUnit unit,
      int reorderPoint,
      int reorderQuantity) {
    Item item = new Item();
    item.name = name;
    item.description = description;
    item.costPrice = costPrice;
    item.category = category;
    item.unit = unit;
    item.reorderPoint = reorderPoint;
    item.reorderQuantity = reorderQuantity;
    item.status = ItemStatus.ACTIVE;
    item.createdAt = LocalDateTime.now();
    item.updatedAt = item.createdAt;
    return item;
  }

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

  public String getSaleSku() {
    return saleSku != null ? saleSku : "";
  }

  public void setSaleSku(String saleSku) {
    this.saleSku = saleSku;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public ItemKind getKind() {
    return productId != null ? ItemKind.PRODUCT : ItemKind.WAREHOUSE;
  }

  public boolean isLinkedToProduct() {
    return productId != null;
  }
}
