package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollAdjustmentType;
import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
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
    name = "payroll_adjustments",
    indexes = {
      @Index(name = "idx_payroll_adjustments_payroll_record_id", columnList = "payroll_record_id"),
      @Index(name = "idx_payroll_adjustments_deleted_at", columnList = "deleted_at")
    })
public class PayrollAdjustmentJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "payroll_record_id", nullable = false)
  private Long payrollRecordId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private PayrollAdjustmentType type;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(nullable = false, length = 500)
  private String reason;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getPayrollRecordId() {
    return payrollRecordId;
  }

  public void setPayrollRecordId(Long payrollRecordId) {
    this.payrollRecordId = payrollRecordId;
  }

  public PayrollAdjustmentType getType() {
    return type;
  }

  public void setType(PayrollAdjustmentType type) {
    this.type = type;
  }

  public BigDecimal getAmount() {
    return amount != null ? amount : BigDecimal.ZERO;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount != null ? amount : BigDecimal.ZERO;
  }

  public String getReason() {
    return reason != null ? reason : "";
  }

  public void setReason(String reason) {
    this.reason = reason != null ? reason.trim() : "";
  }
}
