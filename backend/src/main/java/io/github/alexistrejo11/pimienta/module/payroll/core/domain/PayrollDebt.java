package io.github.alexistrejo11.pimienta.module.payroll.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollDebt extends BaseDomain<Long> {

  private Long employeeId;
  private Long payrollRecordId;
  private BigDecimal amountOwed;
  private String reason;
  private boolean settled;
  private LocalDateTime settledAt;

  private PayrollDebt() {
    this.id = 0L;
    this.amountOwed = BigDecimal.ZERO;
    this.reason = "";
    this.settled = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public Long getEmployeeId() { return employeeId; }
  public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
  public Long getPayrollRecordId() { return payrollRecordId; }
  public void setPayrollRecordId(Long payrollRecordId) { this.payrollRecordId = payrollRecordId; }
  public BigDecimal getAmountOwed() { return amountOwed != null ? amountOwed : BigDecimal.ZERO; }
  public void setAmountOwed(BigDecimal amountOwed) { this.amountOwed = amountOwed != null ? amountOwed : BigDecimal.ZERO; }
  public String getReason() { return reason != null ? reason : ""; }
  public void setReason(String reason) { this.reason = reason != null ? reason.trim() : ""; }
  public boolean isSettled() { return settled; }
  public void setSettled(boolean settled) { this.settled = settled; }
  public LocalDateTime getSettledAt() { return settledAt; }
  public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }

  public static final class SafeBuilder {
    private Long id;
    private Long employeeId;
    private Long payrollRecordId;
    private BigDecimal amountOwed;
    private String reason;
    private boolean settled;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) { this.id = id; return this; }
    public SafeBuilder withEmployeeId(Long employeeId) { this.employeeId = employeeId; return this; }
    public SafeBuilder withPayrollRecordId(Long payrollRecordId) { this.payrollRecordId = payrollRecordId; return this; }
    public SafeBuilder withAmountOwed(BigDecimal amountOwed) { this.amountOwed = amountOwed; return this; }
    public SafeBuilder withReason(String reason) { this.reason = reason; return this; }
    public SafeBuilder withSettled(boolean settled) { this.settled = settled; return this; }
    public SafeBuilder withSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; return this; }
    public SafeBuilder withCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
    public SafeBuilder withVersion(Long version) { this.version = version; return this; }

    public PayrollDebt reconstruct() {
      PayrollDebt debt = new PayrollDebt();
      debt.id = id != null ? id : 0L;
      debt.employeeId = employeeId;
      debt.payrollRecordId = payrollRecordId;
      debt.amountOwed = amountOwed != null ? amountOwed : BigDecimal.ZERO;
      debt.reason = reason != null ? reason.trim() : "";
      debt.settled = settled;
      debt.settledAt = settledAt;
      debt.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      debt.updatedAt = updatedAt != null ? updatedAt : debt.createdAt;
      debt.deletedAt = deletedAt;
      debt.version = version;
      return debt;
    }

    public PayrollDebt register() { return reconstruct(); }
  }
}
