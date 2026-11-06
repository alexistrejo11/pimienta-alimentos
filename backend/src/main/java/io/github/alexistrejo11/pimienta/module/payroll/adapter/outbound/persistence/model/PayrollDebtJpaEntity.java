package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payroll_debts",
    indexes = {
      @Index(name = "idx_payroll_debts_employee_id", columnList = "employee_id"),
      @Index(name = "idx_payroll_debts_settled", columnList = "settled"),
      @Index(name = "idx_payroll_debts_deleted_at", columnList = "deleted_at")
    })
public class PayrollDebtJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Column(name = "payroll_record_id")
  private Long payrollRecordId;

  @Column(name = "amount_owed", nullable = false, precision = 19, scale = 4)
  private BigDecimal amountOwed;

  @Column(length = 500)
  private String reason;

  @Column(nullable = false)
  private boolean settled;

  @Column(name = "settled_at")
  private LocalDateTime settledAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }

  public Long getPayrollRecordId() {
    return payrollRecordId;
  }

  public void setPayrollRecordId(Long payrollRecordId) {
    this.payrollRecordId = payrollRecordId;
  }

  public BigDecimal getAmountOwed() {
    return amountOwed != null ? amountOwed : BigDecimal.ZERO;
  }

  public void setAmountOwed(BigDecimal amountOwed) {
    this.amountOwed = amountOwed != null ? amountOwed : BigDecimal.ZERO;
  }

  public String getReason() {
    return reason != null ? reason : "";
  }

  public void setReason(String reason) {
    this.reason = reason != null ? reason.trim() : "";
  }

  public boolean isSettled() {
    return settled;
  }

  public void setSettled(boolean settled) {
    this.settled = settled;
  }

  public LocalDateTime getSettledAt() {
    return settledAt;
  }

  public void setSettledAt(LocalDateTime settledAt) {
    this.settledAt = settledAt;
  }
}
