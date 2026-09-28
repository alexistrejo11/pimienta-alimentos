package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
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
import org.hibernate.annotations.Formula;

@Entity
@Table(
    name = "inventory_items",
    indexes = {
      @Index(name = "idx_inventory_items_status", columnList = "status"),
      @Index(name = "idx_inventory_items_deleted_at", columnList = "deleted_at")
    })
public class ItemJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  @Column(name = "cost_price", nullable = false, precision = 19, scale = 6)
  private BigDecimal costPrice;

  @Column(name = "reorder_point", nullable = false)
  private int reorderPoint;

  @Column(name = "reorder_quantity", nullable = false)
  private int reorderQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ItemStatus status;

  @Formula(
      "(select p.id from products p where p.inventory_item_id = id and p.deleted_at is null)")
  private Long productId;

  @Formula(
      "(select p.sku from products p where p.inventory_item_id = id and p.deleted_at is null)")
  private String saleSku;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getProductId() {
    return productId;
  }

  public String getSaleSku() {
    return saleSku;
  }
}
