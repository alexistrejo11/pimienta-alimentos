package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.CatalogRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "inventory_items",
    indexes = {
      @Index(name = "idx_inventory_items_barcode", columnList = "barcode"),
      @Index(name = "idx_inventory_items_status", columnList = "status"),
      @Index(name = "idx_inventory_items_deleted_at", columnList = "deleted_at")
    })
public class ItemJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String sku;

  @Column(nullable = false, length = 300)
  private String name;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ItemCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ItemUnit unit;

  @Column(length = 120)
  private String brand;

  @Column(length = 64)
  private String barcode;

  @Column(name = "cost_price", nullable = false, precision = 19, scale = 6)
  private BigDecimal costPrice;

  @Column(name = "sale_price", nullable = false, precision = 19, scale = 6)
  private BigDecimal salePrice;

  @Column(name = "reorder_point", nullable = false)
  private int reorderPoint;

  @Column(name = "reorder_quantity", nullable = false)
  private int reorderQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ItemStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_role", nullable = false, length = 32)
  private CatalogRole catalogRole;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(nullable = false)
  private Long version;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }

  public BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(BigDecimal salePrice) {
    this.salePrice = salePrice;
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

  public CatalogRole getCatalogRole() { return catalogRole; }
  public void setCatalogRole(CatalogRole catalogRole) { this.catalogRole = catalogRole; }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
