package io.github.alexistrejo11.pimienta.module.headquarter.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** POS operational settings for a headquarter (1:1). */
public class PosOperationalConfig extends BaseDomain<Long> {

  private Long headquarterId;
  private String currency;
  private int catalogStaleWarnHours;
  private int catalogStaleBlockHours;
  private List<String> openAmountCategories;
  private boolean allowOpenProducts;
  private Integer defaultNegativeStockLimit;

  private PosOperationalConfig() {
    this.id = 0L;
    this.headquarterId = 0L;
    this.currency = "MXN";
    this.catalogStaleWarnHours = 24;
    this.catalogStaleBlockHours = 72;
    this.openAmountCategories = new ArrayList<>();
    this.allowOpenProducts = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public String getCurrency() {
    return currency != null ? currency : "MXN";
  }

  public int getCatalogStaleWarnHours() {
    return catalogStaleWarnHours;
  }

  public int getCatalogStaleBlockHours() {
    return catalogStaleBlockHours;
  }

  public List<String> getOpenAmountCategories() {
    return openAmountCategories != null ? List.copyOf(openAmountCategories) : List.of();
  }

  public Integer getDefaultNegativeStockLimit() {
    return defaultNegativeStockLimit;
  }

  public boolean isAllowOpenProducts() {
    return allowOpenProducts;
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private Long headquarterId;
    private String currency;
    private Integer catalogStaleWarnHours;
    private Integer catalogStaleBlockHours;
    private List<String> openAmountCategories;
    private Boolean allowOpenProducts;
    private Integer defaultNegativeStockLimit;
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

    public SafeBuilder withCurrency(String currency) {
      this.currency = currency;
      return this;
    }

    public SafeBuilder withCatalogStaleWarnHours(Integer hours) {
      this.catalogStaleWarnHours = hours;
      return this;
    }

    public SafeBuilder withCatalogStaleBlockHours(Integer hours) {
      this.catalogStaleBlockHours = hours;
      return this;
    }

    public SafeBuilder withOpenAmountCategories(List<String> categories) {
      this.openAmountCategories = categories;
      return this;
    }

    public SafeBuilder withAllowOpenProducts(Boolean allowOpenProducts) {
      this.allowOpenProducts = allowOpenProducts;
      return this;
    }

    public SafeBuilder withDefaultNegativeStockLimit(Integer limit) {
      this.defaultNegativeStockLimit = limit;
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

    public PosOperationalConfig reconstruct() {
      PosOperationalConfig c = new PosOperationalConfig();
      c.id = id != null ? id : 0L;
      c.headquarterId = headquarterId != null ? headquarterId : 0L;
      c.currency = currency != null && !currency.isBlank() ? currency.strip() : "MXN";
      c.catalogStaleWarnHours = catalogStaleWarnHours != null ? catalogStaleWarnHours : 24;
      c.catalogStaleBlockHours = catalogStaleBlockHours != null ? catalogStaleBlockHours : 72;
      c.openAmountCategories =
          openAmountCategories != null ? new ArrayList<>(openAmountCategories) : new ArrayList<>();
      c.allowOpenProducts = allowOpenProducts != null && allowOpenProducts;
      c.defaultNegativeStockLimit = defaultNegativeStockLimit;
      c.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      c.updatedAt = updatedAt != null ? updatedAt : c.createdAt;
      c.deletedAt = deletedAt;
      c.version = version != null ? version : 0L;
      return c;
    }

    public PosOperationalConfig register() {
      var now = LocalDateTime.now();
      PosOperationalConfig c = reconstruct();
      c.id = 0L;
      c.createdAt = now;
      c.updatedAt = now;
      c.deletedAt = null;
      c.version = 0L;
      return c;
    }

    public PosOperationalConfig revise(PosOperationalConfig existing) {
      PosOperationalConfig c = reconstruct();
      c.id = existing.getId();
      c.headquarterId = existing.getHeadquarterId();
      c.createdAt = existing.getCreatedAt();
      c.deletedAt = existing.getDeletedAt();
      c.updatedAt = LocalDateTime.now();
      c.version = existing.getVersion() != null ? existing.getVersion() : 0L;
      return c;
    }
  }
}
