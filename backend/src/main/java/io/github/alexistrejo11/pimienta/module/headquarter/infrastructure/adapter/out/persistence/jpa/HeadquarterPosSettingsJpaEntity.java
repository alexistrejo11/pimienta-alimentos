package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "headquarter_pos_settings",
    indexes = {
      @Index(name = "idx_headquarter_pos_settings_deleted_at", columnList = "deleted_at")
    })
public class HeadquarterPosSettingsJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "headquarter_id", nullable = false, unique = true)
  private Long headquarterId;

  @Column(nullable = false, length = 8)
  private String currency;

  @Column(name = "catalog_stale_warn_hours", nullable = false)
  private int catalogStaleWarnHours;

  @Column(name = "catalog_stale_block_hours", nullable = false)
  private int catalogStaleBlockHours;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "open_amount_categories", nullable = false)
  private List<String> openAmountCategories = new ArrayList<>();

  @Column(name = "allow_open_products", nullable = false)
  private boolean allowOpenProducts;

  @Column(name = "default_negative_stock_limit")
  private Integer defaultNegativeStockLimit;

  @Column(name = "stockless", nullable = false)
  private boolean stockless;

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

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getCatalogStaleWarnHours() {
    return catalogStaleWarnHours;
  }

  public void setCatalogStaleWarnHours(int catalogStaleWarnHours) {
    this.catalogStaleWarnHours = catalogStaleWarnHours;
  }

  public int getCatalogStaleBlockHours() {
    return catalogStaleBlockHours;
  }

  public void setCatalogStaleBlockHours(int catalogStaleBlockHours) {
    this.catalogStaleBlockHours = catalogStaleBlockHours;
  }

  public List<String> getOpenAmountCategories() {
    return openAmountCategories;
  }

  public void setOpenAmountCategories(List<String> openAmountCategories) {
    this.openAmountCategories =
        openAmountCategories != null ? openAmountCategories : new ArrayList<>();
  }

  public boolean isAllowOpenProducts() {
    return allowOpenProducts;
  }

  public void setAllowOpenProducts(boolean allowOpenProducts) {
    this.allowOpenProducts = allowOpenProducts;
  }

  public Integer getDefaultNegativeStockLimit() {
    return defaultNegativeStockLimit;
  }

  public void setDefaultNegativeStockLimit(Integer defaultNegativeStockLimit) {
    this.defaultNegativeStockLimit = defaultNegativeStockLimit;
  }

  public boolean isStockless() {
    return stockless;
  }

  public void setStockless(boolean stockless) {
    this.stockless = stockless;
  }
}
