package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollRecordStatus;
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
import java.time.LocalDate;

@Entity
@Table(
    name = "payroll_records",
    indexes = {
      @Index(name = "idx_payroll_records_employee_id", columnList = "employee_id"),
      @Index(name = "idx_payroll_records_period_id", columnList = "period_id"),
      @Index(name = "idx_payroll_records_status", columnList = "status"),
      @Index(name = "idx_payroll_records_deleted_at", columnList = "deleted_at")
    })
public class PayrollRecordJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Column(name = "period_id")
  private Long periodId;

  @Column(name = "worked_days_start", nullable = false)
  private LocalDate workedDaysStart;

  @Column(name = "worked_days_end", nullable = false)
  private LocalDate workedDaysEnd;

  @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal grossAmount;

  @Column(name = "total_discounts", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalDiscounts;

  @Column(name = "total_bonuses", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalBonuses;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal netAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PayrollRecordStatus status;

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

  public Long getPeriodId() {
    return periodId;
  }

  public void setPeriodId(Long periodId) {
    this.periodId = periodId;
  }

  public LocalDate getWorkedDaysStart() {
    return workedDaysStart;
  }

  public void setWorkedDaysStart(LocalDate workedDaysStart) {
    this.workedDaysStart = workedDaysStart;
  }

  public LocalDate getWorkedDaysEnd() {
    return workedDaysEnd;
  }

  public void setWorkedDaysEnd(LocalDate workedDaysEnd) {
    this.workedDaysEnd = workedDaysEnd;
  }

  public BigDecimal getGrossAmount() {
    return grossAmount != null ? grossAmount : BigDecimal.ZERO;
  }

  public void setGrossAmount(BigDecimal grossAmount) {
    this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
  }

  public BigDecimal getTotalDiscounts() {
    return totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO;
  }

  public void setTotalDiscounts(BigDecimal totalDiscounts) {
    this.totalDiscounts = totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO;
  }

  public BigDecimal getTotalBonuses() {
    return totalBonuses != null ? totalBonuses : BigDecimal.ZERO;
  }

  public void setTotalBonuses(BigDecimal totalBonuses) {
    this.totalBonuses = totalBonuses != null ? totalBonuses : BigDecimal.ZERO;
  }

  public BigDecimal getNetAmount() {
    return netAmount != null ? netAmount : BigDecimal.ZERO;
  }

  public void setNetAmount(BigDecimal netAmount) {
    this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
  }

  public PayrollRecordStatus getStatus() {
    return status != null ? status : PayrollRecordStatus.PENDING;
  }

  public void setStatus(PayrollRecordStatus status) {
    this.status = status;
  }
}
