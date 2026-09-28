package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
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
import java.math.BigDecimal;

@Entity
@Table(
    name = "headquarter_items",
    indexes = {
      @Index(name = "idx_headquarter_items_headquarter_id", columnList = "headquarter_id"),
      @Index(name = "idx_headquarter_items_product_id", columnList = "product_id"),
      @Index(name = "idx_headquarter_items_deleted_at", columnList = "deleted_at")
    })
public class HeadquarterItemJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "pos_sale_category_id", nullable = false)
  private Long posSaleCategoryId;

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

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getSaleCategory() {
    return saleCategory;
  }

  public Long getPosSaleCategoryId() {
    return posSaleCategoryId;
  }

  public void setPosSaleCategoryId(Long posSaleCategoryId) {
    this.posSaleCategoryId = posSaleCategoryId;
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
}
