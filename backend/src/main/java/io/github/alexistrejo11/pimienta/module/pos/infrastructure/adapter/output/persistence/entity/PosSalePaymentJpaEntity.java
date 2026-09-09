package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosPaymentMethod;
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
@Table(name = "pos_sale_payments")
public class PosSalePaymentJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sale_id", nullable = false)
  private PosSaleJpaEntity sale;

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PosPaymentMethod method;

  @Column(name = "amount_centavos", nullable = false)
  private long amountCentavos;

  @Column(name = "tendered_centavos")
  private Long tenderedCentavos;

  @Column(name = "change_centavos")
  private Long changeCentavos;

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

  public UUID getPaymentId() {
    return paymentId;
  }

  public void setPaymentId(UUID paymentId) {
    this.paymentId = paymentId;
  }

  public PosPaymentMethod getMethod() {
    return method;
  }

  public void setMethod(PosPaymentMethod method) {
    this.method = method;
  }

  public long getAmountCentavos() {
    return amountCentavos;
  }

  public void setAmountCentavos(long amountCentavos) {
    this.amountCentavos = amountCentavos;
  }

  public Long getTenderedCentavos() {
    return tenderedCentavos;
  }

  public void setTenderedCentavos(Long tenderedCentavos) {
    this.tenderedCentavos = tenderedCentavos;
  }

  public Long getChangeCentavos() {
    return changeCentavos;
  }

  public void setChangeCentavos(Long changeCentavos) {
    this.changeCentavos = changeCentavos;
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
