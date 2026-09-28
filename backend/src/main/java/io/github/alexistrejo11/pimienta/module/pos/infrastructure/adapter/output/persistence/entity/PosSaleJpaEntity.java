package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "pos_sales")
public class PosSaleJpaEntity extends BaseJpaEntity implements Persistable<UUID> {

  @Id
  @Column(name = "sale_id")
  private UUID saleId;

  @Transient private boolean newEntity = true;

  @Column(name = "event_id", nullable = false, unique = true)
  private UUID eventId;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "device_id", nullable = false)
  private UUID deviceId;

  @Column(name = "shift_id")
  private UUID shiftId;

  @Column(name = "cashier_operator_id")
  private Long cashierOperatorId;

  @Column(nullable = false, length = 64)
  private String folio;

  @Column(name = "gross_centavos", nullable = false)
  private long grossCentavos;

  @Column(name = "discount_centavos", nullable = false)
  private long discountCentavos;

  @Column(name = "total_centavos", nullable = false)
  private long totalCentavos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PosSaleStatus status;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @OneToMany(
      mappedBy = "sale",
       cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private List<PosSaleLineJpaEntity> lines = new ArrayList<>();

  @OneToMany(
      mappedBy = "sale",
       cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private List<PosSalePaymentJpaEntity> payments = new ArrayList<>();

  @Override
  public UUID getId() {
    return saleId;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  public void setNewEntity(boolean newEntity) {
    this.newEntity = newEntity;
  }

  public UUID getSaleId() {
    return saleId;
  }

  public void setSaleId(UUID saleId) {
    this.saleId = saleId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(UUID deviceId) {
    this.deviceId = deviceId;
  }

  public UUID getShiftId() {
    return shiftId;
  }

  public void setShiftId(UUID shiftId) {
    this.shiftId = shiftId;
  }

  public Long getCashierOperatorId() {
    return cashierOperatorId;
  }

  public void setCashierOperatorId(Long cashierOperatorId) {
    this.cashierOperatorId = cashierOperatorId;
  }

  public String getFolio() {
    return folio;
  }

  public void setFolio(String folio) {
    this.folio = folio;
  }

  public long getGrossCentavos() {
    return grossCentavos;
  }

  public void setGrossCentavos(long grossCentavos) {
    this.grossCentavos = grossCentavos;
  }

  public long getDiscountCentavos() {
    return discountCentavos;
  }

  public void setDiscountCentavos(long discountCentavos) {
    this.discountCentavos = discountCentavos;
  }

  public long getTotalCentavos() {
    return totalCentavos;
  }

  public void setTotalCentavos(long totalCentavos) {
    this.totalCentavos = totalCentavos;
  }

  public PosSaleStatus getStatus() {
    return status;
  }

  public void setStatus(PosSaleStatus status) {
    this.status = status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public List<PosSaleLineJpaEntity> getLines() {
    return lines;
  }

  public void setLines(List<PosSaleLineJpaEntity> lines) {
    this.lines = lines;
  }

  public List<PosSalePaymentJpaEntity> getPayments() {
    return payments;
  }

  public void setPayments(List<PosSalePaymentJpaEntity> payments) {
    this.payments = payments;
  }
}
