package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "headquarter_items",
    indexes = {
      @Index(name = "idx_headquarter_items_headquarter_id", columnList = "headquarter_id"),
      @Index(name = "idx_headquarter_items_item_id", columnList = "item_id"),
      @Index(name = "idx_headquarter_items_deleted_at", columnList = "deleted_at")
    })
public class HeadquarterItemJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "sale_category", nullable = false, length = 64)
  private String saleCategory;

  @Column(name = "sale_price", nullable = false, precision = 19, scale = 6)
  private BigDecimal salePrice;

  @Column(nullable = false)
  private boolean available;

  @Enumerated(EnumType.STRING)
  @Column(name = "stock_policy", nullable = false, length = 32)
  private StockPolicy stockPolicy;

  @Column(name = "negative_stock_limit")
  private Integer negativeStockLimit;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public String getSaleCategory() {
    return saleCategory;
  }

  public void setSaleCategory(String saleCategory) {
    this.saleCategory = saleCategory;
  }

  public BigDecimal getSalePrice() {
    return salePrice;
  }

  public void setSalePrice(BigDecimal salePrice) {
    this.salePrice = salePrice;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public StockPolicy getStockPolicy() {
    return stockPolicy;
  }

  public void setStockPolicy(StockPolicy stockPolicy) {
    this.stockPolicy = stockPolicy;
  }

  public Integer getNegativeStockLimit() {
    return negativeStockLimit;
  }

  public void setNegativeStockLimit(Integer negativeStockLimit) {
    this.negativeStockLimit = negativeStockLimit;
  }

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
