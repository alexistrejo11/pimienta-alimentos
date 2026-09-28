package io.github.alexistrejo11.pimienta.module.payroll.core.domain;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollRecordStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollPayment extends BaseDomain<Long> {

  private Long payrollRecordId;
  private Long employeeId;
  private PayrollFrequency frequency;
  private LocalDate workedDaysStart;
  private LocalDate workedDaysEnd;
  private BigDecimal grossAmount;
  private BigDecimal netAmount;
  private String destinationAccount;
  private String transactionId;
  private PayrollRecordStatus status;
  private BigDecimal pendingAmount;

  private PayrollPayment() {
    this.id = 0L;
    this.frequency = PayrollFrequency.CUSTOM;
    this.grossAmount = BigDecimal.ZERO;
    this.netAmount = BigDecimal.ZERO;
    this.pendingAmount = BigDecimal.ZERO;
    this.destinationAccount = "";
    this.transactionId = "";
    this.status = PayrollRecordStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public Long getPayrollRecordId() { return payrollRecordId; }
  public void setPayrollRecordId(Long payrollRecordId) { this.payrollRecordId = payrollRecordId; }
  public Long getEmployeeId() { return employeeId; }
  public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
  public PayrollFrequency getFrequency() { return frequency != null ? frequency : PayrollFrequency.CUSTOM; }
  public void setFrequency(PayrollFrequency frequency) { this.frequency = frequency != null ? frequency : PayrollFrequency.CUSTOM; }
  public LocalDate getWorkedDaysStart() { return workedDaysStart; }
  public void setWorkedDaysStart(LocalDate workedDaysStart) { this.workedDaysStart = workedDaysStart; }
  public LocalDate getWorkedDaysEnd() { return workedDaysEnd; }
  public void setWorkedDaysEnd(LocalDate workedDaysEnd) { this.workedDaysEnd = workedDaysEnd; }
  public BigDecimal getGrossAmount() { return grossAmount != null ? grossAmount : BigDecimal.ZERO; }
  public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO; }
  public BigDecimal getNetAmount() { return netAmount != null ? netAmount : BigDecimal.ZERO; }
  public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO; }
  public String getDestinationAccount() { return destinationAccount != null ? destinationAccount : ""; }
  public void setDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount != null ? destinationAccount.trim() : ""; }
  public String getTransactionId() { return transactionId != null ? transactionId : ""; }
  public void setTransactionId(String transactionId) { this.transactionId = transactionId != null ? transactionId.trim() : ""; }
  public PayrollRecordStatus getStatus() { return status != null ? status : PayrollRecordStatus.PENDING; }
  public void setStatus(PayrollRecordStatus status) { this.status = status != null ? status : PayrollRecordStatus.PENDING; }
  public BigDecimal getPendingAmount() { return pendingAmount != null ? pendingAmount : BigDecimal.ZERO; }
  public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO; }

  public static final class SafeBuilder {
    private Long id;
    private Long payrollRecordId;
    private Long employeeId;
    private PayrollFrequency frequency;
    private LocalDate workedDaysStart;
    private LocalDate workedDaysEnd;
    private BigDecimal grossAmount;
    private BigDecimal netAmount;
    private String destinationAccount;
    private String transactionId;
    private PayrollRecordStatus status;
    private BigDecimal pendingAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) { this.id = id; return this; }
    public SafeBuilder withPayrollRecordId(Long payrollRecordId) { this.payrollRecordId = payrollRecordId; return this; }
    public SafeBuilder withEmployeeId(Long employeeId) { this.employeeId = employeeId; return this; }
    public SafeBuilder withFrequency(PayrollFrequency frequency) { this.frequency = frequency; return this; }
    public SafeBuilder withWorkedDaysStart(LocalDate workedDaysStart) { this.workedDaysStart = workedDaysStart; return this; }
    public SafeBuilder withWorkedDaysEnd(LocalDate workedDaysEnd) { this.workedDaysEnd = workedDaysEnd; return this; }
    public SafeBuilder withGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; return this; }
    public SafeBuilder withNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }
    public SafeBuilder withDestinationAccount(String destinationAccount) { this.destinationAccount = destinationAccount; return this; }
    public SafeBuilder withTransactionId(String transactionId) { this.transactionId = transactionId; return this; }
    public SafeBuilder withStatus(PayrollRecordStatus status) { this.status = status; return this; }
    public SafeBuilder withPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; return this; }
    public SafeBuilder withCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
    public SafeBuilder withVersion(Long version) { this.version = version; return this; }

    public PayrollPayment reconstruct() {
      PayrollPayment payment = new PayrollPayment();
      payment.id = id != null ? id : 0L;
      payment.payrollRecordId = payrollRecordId;
      payment.employeeId = employeeId;
      payment.frequency = frequency != null ? frequency : PayrollFrequency.CUSTOM;
      payment.workedDaysStart = workedDaysStart;
      payment.workedDaysEnd = workedDaysEnd;
      payment.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
      payment.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
      payment.destinationAccount = destinationAccount != null ? destinationAccount.trim() : "";
      payment.transactionId = transactionId != null ? transactionId.trim() : "";
      payment.status = status != null ? status : PayrollRecordStatus.PENDING;
      payment.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
      payment.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      payment.updatedAt = updatedAt != null ? updatedAt : payment.createdAt;
      payment.deletedAt = deletedAt;
      payment.version = version;
      return payment;
    }

    public PayrollPayment register() { return reconstruct(); }
  }
}
