package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pos_sale_lines")
public class PosSaleLineJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false)
  private PosSaleJpaEntity sale;

  @Column(name = "line_id", nullable = false)
  private UUID lineId;

  @Enumerated(EnumType.STRING)
  @Column(name = "line_type", nullable = false, length = 32)
  private PosSaleLineType lineType;

  @Column(name = "product_id")
  private Long productId;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "sale_category", length = 64)
  private String saleCategory;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false, length = 32)
  private String unit;

  @Column(name = "unit_price_centavos", nullable = false)
  private long unitPriceCentavos;

  @Column(name = "subtotal_centavos", nullable = false)
  private long subtotalCentavos;

  @Enumerated(EnumType.STRING)
  @Column(name = "stock_policy", nullable = false, length = 32)
  private PosSaleStockPolicy stockPolicy;

  @Column(name = "sold_with_negative_stock", nullable = false)
  private boolean soldWithNegativeStock;

  @Column(name = "sold_while_unavailable", nullable = false)
  private boolean soldWhileUnavailable;

  @Column(name = "raw_barcode", length = 128)
  private String rawBarcode;

  @Column(name = "authorized_by_operator_id")
  private Long authorizedByOperatorId;

  @Column(name = "authorized_at")
  private java.time.Instant authorizedAt;

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

  public PosSaleJpaEntity getSale() {
    return sale;
  }

  public void setSale(PosSaleJpaEntity sale) {
    this.sale = sale;
  }

  public UUID getLineId() {
    return lineId;
  }

  public void setLineId(UUID lineId) {
    this.lineId = lineId;
  }

  public PosSaleLineType getLineType() { return lineType; }
  public void setLineType(PosSaleLineType lineType) { this.lineType = lineType; }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getSaleCategory() {
    return saleCategory;
  }

  public void setSaleCategory(String saleCategory) {
    this.saleCategory = saleCategory;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public long getUnitPriceCentavos() {
    return unitPriceCentavos;
  }

  public void setUnitPriceCentavos(long unitPriceCentavos) {
    this.unitPriceCentavos = unitPriceCentavos;
  }

  public long getSubtotalCentavos() {
    return subtotalCentavos;
  }

  public void setSubtotalCentavos(long subtotalCentavos) {
    this.subtotalCentavos = subtotalCentavos;
  }

  public PosSaleStockPolicy getStockPolicy() {
    return stockPolicy;
  }

  public void setStockPolicy(PosSaleStockPolicy stockPolicy) {
    this.stockPolicy = stockPolicy;
  }

  public boolean isSoldWithNegativeStock() {
    return soldWithNegativeStock;
  }

  public void setSoldWithNegativeStock(boolean soldWithNegativeStock) {
    this.soldWithNegativeStock = soldWithNegativeStock;
  }

  public boolean isSoldWhileUnavailable() {
    return soldWhileUnavailable;
  }

  public void setSoldWhileUnavailable(boolean soldWhileUnavailable) {
    this.soldWhileUnavailable = soldWhileUnavailable;
  }

  public String getRawBarcode() {
    return rawBarcode;
  }

  public void setRawBarcode(String rawBarcode) {
    this.rawBarcode = rawBarcode;
  }

  public Long getAuthorizedByOperatorId() { return authorizedByOperatorId; }
  public void setAuthorizedByOperatorId(Long id) { this.authorizedByOperatorId = id; }
  public java.time.Instant getAuthorizedAt() { return authorizedAt; }
  public void setAuthorizedAt(java.time.Instant at) { this.authorizedAt = at; }

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
