package io.github.alexistrejo11.pimienta.module.headquarter.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Effective POS catalog row for one item at one headquarter. */
public class HeadquarterItem extends BaseDomain<Long> {

  public enum StockPolicy {
    CONTROLLED,
    NOT_CONTROLLED
  }

  private Long headquarterId;
  private Long itemId;
  private String saleCategory;
  private BigDecimal salePrice;
  private boolean available;
  private StockPolicy stockPolicy;
  private Integer negativeStockLimit;

  private HeadquarterItem() {
    this.id = 0L;
    this.headquarterId = 0L;
    this.itemId = 0L;
    this.saleCategory = "";
    this.salePrice = BigDecimal.ZERO;
    this.available = true;
    this.stockPolicy = StockPolicy.CONTROLLED;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public Long getItemId() {
    return itemId;
  }

  public String getSaleCategory() {
    return saleCategory != null ? saleCategory : "";
  }

  public BigDecimal getSalePrice() {
    return salePrice != null ? salePrice : BigDecimal.ZERO;
  }

  public boolean isAvailable() {
    return available;
  }

  public StockPolicy getStockPolicy() {
    return stockPolicy != null ? stockPolicy : StockPolicy.CONTROLLED;
  }

  public Integer getNegativeStockLimit() {
    return negativeStockLimit;
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.updatedAt = this.deletedAt;
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private Long headquarterId;
    private Long itemId;
    private String saleCategory;
    private BigDecimal salePrice;
    private Boolean available;
    private StockPolicy stockPolicy;
    private Integer negativeStockLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withItemId(Long itemId) {
      this.itemId = itemId;
      return this;
    }

    public SafeBuilder withSaleCategory(String saleCategory) {
      this.saleCategory = saleCategory;
      return this;
    }

    public SafeBuilder withSalePrice(BigDecimal salePrice) {
      this.salePrice = salePrice;
      return this;
    }

    public SafeBuilder withAvailable(Boolean available) {
      this.available = available;
      return this;
    }

    public SafeBuilder withStockPolicy(StockPolicy stockPolicy) {
      this.stockPolicy = stockPolicy;
      return this;
    }

    public SafeBuilder withNegativeStockLimit(Integer negativeStockLimit) {
      this.negativeStockLimit = negativeStockLimit;
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public HeadquarterItem reconstruct() {
      HeadquarterItem h = new HeadquarterItem();
      h.id = id != null ? id : 0L;
      h.headquarterId = headquarterId != null ? headquarterId : 0L;
      h.itemId = itemId != null ? itemId : 0L;
      h.saleCategory = saleCategory != null ? saleCategory.strip() : "";
      h.salePrice = salePrice != null ? salePrice : BigDecimal.ZERO;
      h.available = available == null || available;
      h.stockPolicy = stockPolicy != null ? stockPolicy : StockPolicy.CONTROLLED;
      h.negativeStockLimit = negativeStockLimit;
      h.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      h.updatedAt = updatedAt != null ? updatedAt : h.createdAt;
      h.deletedAt = deletedAt;
      h.version = version != null ? version : 0L;
      return h;
    }

    public HeadquarterItem register() {
      var now = LocalDateTime.now();
      HeadquarterItem h = reconstruct();
      h.id = 0L;
      h.createdAt = now;
      h.updatedAt = now;
      h.deletedAt = null;
      h.version = 0L;
      return h;
    }

    public HeadquarterItem revise(HeadquarterItem existing) {
      HeadquarterItem h = reconstruct();
      h.id = existing.getId();
      h.headquarterId = existing.getHeadquarterId();
      h.itemId = existing.getItemId();
      h.createdAt = existing.getCreatedAt();
      h.deletedAt = existing.getDeletedAt();
      h.updatedAt = LocalDateTime.now();
      h.version = existing.getVersion() != null ? existing.getVersion() : 0L;
      return h;
    }
  }
}
