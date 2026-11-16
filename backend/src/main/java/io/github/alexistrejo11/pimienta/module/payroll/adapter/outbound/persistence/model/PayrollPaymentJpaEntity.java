package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
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
    name = "payroll_payments",
    indexes = {
      @Index(name = "idx_payroll_payments_payroll_record_id", columnList = "payroll_record_id"),
      @Index(name = "idx_payroll_payments_employee_id", columnList = "employee_id"),
      @Index(name = "idx_payroll_payments_status", columnList = "status"),
      @Index(name = "idx_payroll_payments_deleted_at", columnList = "deleted_at")
    })
public class PayrollPaymentJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "payroll_record_id")
  private Long payrollRecordId;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PayrollFrequency frequency;

  @Column(name = "worked_days_start", nullable = false)
  private LocalDate workedDaysStart;

  @Column(name = "worked_days_end", nullable = false)
  private LocalDate workedDaysEnd;

  @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal grossAmount;

  @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal netAmount;

  @Column(name = "destination_account", nullable = false, length = 128)
  private String destinationAccount;

  @Column(name = "transaction_id", nullable = false, length = 128)
  private String transactionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PayrollRecordStatus status;

  @Column(name = "pending_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal pendingAmount;

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

  public Long getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }

  public PayrollFrequency getFrequency() {
    return frequency != null ? frequency : PayrollFrequency.CUSTOM;
  }

  public void setFrequency(PayrollFrequency frequency) {
    this.frequency = frequency;
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

  public BigDecimal getNetAmount() {
    return netAmount != null ? netAmount : BigDecimal.ZERO;
  }

  public void setNetAmount(BigDecimal netAmount) {
    this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
  }

  public String getDestinationAccount() {
    return destinationAccount != null ? destinationAccount : "";
  }

  public void setDestinationAccount(String destinationAccount) {
    this.destinationAccount = destinationAccount != null ? destinationAccount.trim() : "";
  }

  public String getTransactionId() {
    return transactionId != null ? transactionId : "";
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId != null ? transactionId.trim() : "";
  }

  public PayrollRecordStatus getStatus() {
    return status != null ? status : PayrollRecordStatus.PENDING;
  }

  public void setStatus(PayrollRecordStatus status) {
    this.status = status;
  }

  public BigDecimal getPendingAmount() {
    return pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
  }

  public void setPendingAmount(BigDecimal pendingAmount) {
    this.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
  }
}
