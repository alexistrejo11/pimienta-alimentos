package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
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
public class HeadquarterPosSettingsJpaEntity {

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

  @Column(name = "default_negative_stock_limit")
  private Integer defaultNegativeStockLimit;

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

  public Integer getDefaultNegativeStockLimit() {
    return defaultNegativeStockLimit;
  }

  public void setDefaultNegativeStockLimit(Integer defaultNegativeStockLimit) {
    this.defaultNegativeStockLimit = defaultNegativeStockLimit;
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
