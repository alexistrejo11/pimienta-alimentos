package io.github.alexistrejo11.pimienta.module.payroll.core.domain;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollRecordStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollRecord extends BaseDomain<Long> {

  private Long employeeId;
  private Long periodId;
  private LocalDate workedDaysStart;
  private LocalDate workedDaysEnd;
  private BigDecimal grossAmount;
  private BigDecimal totalDiscounts;
  private BigDecimal totalBonuses;
  private BigDecimal netAmount;
  private PayrollRecordStatus status;

  private PayrollRecord() {
    this.id = 0L;
    this.grossAmount = BigDecimal.ZERO;
    this.totalDiscounts = BigDecimal.ZERO;
    this.totalBonuses = BigDecimal.ZERO;
    this.netAmount = BigDecimal.ZERO;
    this.status = PayrollRecordStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
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
    this.status = status != null ? status : PayrollRecordStatus.PENDING;
  }

  public static final class SafeBuilder {
    private Long id;
    private Long employeeId;
    private Long periodId;
    private LocalDate workedDaysStart;
    private LocalDate workedDaysEnd;
    private BigDecimal grossAmount;
    private BigDecimal totalDiscounts;
    private BigDecimal totalBonuses;
    private BigDecimal netAmount;
    private PayrollRecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) { this.id = id; return this; }
    public SafeBuilder withEmployeeId(Long employeeId) { this.employeeId = employeeId; return this; }
    public SafeBuilder withPeriodId(Long periodId) { this.periodId = periodId; return this; }
    public SafeBuilder withWorkedDaysStart(LocalDate workedDaysStart) { this.workedDaysStart = workedDaysStart; return this; }
    public SafeBuilder withWorkedDaysEnd(LocalDate workedDaysEnd) { this.workedDaysEnd = workedDaysEnd; return this; }
    public SafeBuilder withGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; return this; }
    public SafeBuilder withTotalDiscounts(BigDecimal totalDiscounts) { this.totalDiscounts = totalDiscounts; return this; }
    public SafeBuilder withTotalBonuses(BigDecimal totalBonuses) { this.totalBonuses = totalBonuses; return this; }
    public SafeBuilder withNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; return this; }
    public SafeBuilder withStatus(PayrollRecordStatus status) { this.status = status; return this; }
    public SafeBuilder withCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; return this; }
    public SafeBuilder withVersion(Long version) { this.version = version; return this; }

    public PayrollRecord reconstruct() {
      PayrollRecord record = new PayrollRecord();
      record.id = id != null ? id : 0L;
      record.employeeId = employeeId;
      record.periodId = periodId;
      record.workedDaysStart = workedDaysStart;
      record.workedDaysEnd = workedDaysEnd;
      record.grossAmount = grossAmount != null ? grossAmount : BigDecimal.ZERO;
      record.totalDiscounts = totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO;
      record.totalBonuses = totalBonuses != null ? totalBonuses : BigDecimal.ZERO;
      record.netAmount = netAmount != null ? netAmount : BigDecimal.ZERO;
      record.status = status != null ? status : PayrollRecordStatus.PENDING;
      record.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      record.updatedAt = updatedAt != null ? updatedAt : record.createdAt;
      record.deletedAt = deletedAt;
      record.version = version;
      return record;
    }

    public PayrollRecord register() { return reconstruct(); }
  }
}
