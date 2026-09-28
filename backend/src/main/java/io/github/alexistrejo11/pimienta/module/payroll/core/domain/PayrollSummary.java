package io.github.alexistrejo11.pimienta.module.payroll.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollSummary extends BaseDomain<Long> {

  private LocalDate from;
  private LocalDate to;
  private long employeesPaid;
  private BigDecimal totalGross;
  private BigDecimal totalNet;
  private BigDecimal totalDiscounts;
  private BigDecimal totalBonuses;
  private BigDecimal totalDebt;

  public PayrollSummary() {
    this.id = 0L;
    this.totalGross = BigDecimal.ZERO;
    this.totalNet = BigDecimal.ZERO;
    this.totalDiscounts = BigDecimal.ZERO;
    this.totalBonuses = BigDecimal.ZERO;
    this.totalDebt = BigDecimal.ZERO;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public LocalDate getFrom() { return from; }
  public void setFrom(LocalDate from) { this.from = from; }
  public LocalDate getTo() { return to; }
  public void setTo(LocalDate to) { this.to = to; }
  public long getEmployeesPaid() { return employeesPaid; }
  public void setEmployeesPaid(long employeesPaid) { this.employeesPaid = employeesPaid; }
  public BigDecimal getTotalGross() { return totalGross != null ? totalGross : BigDecimal.ZERO; }
  public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross != null ? totalGross : BigDecimal.ZERO; }
  public BigDecimal getTotalNet() { return totalNet != null ? totalNet : BigDecimal.ZERO; }
  public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet != null ? totalNet : BigDecimal.ZERO; }
  public BigDecimal getTotalDiscounts() { return totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO; }
  public void setTotalDiscounts(BigDecimal totalDiscounts) { this.totalDiscounts = totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO; }
  public BigDecimal getTotalBonuses() { return totalBonuses != null ? totalBonuses : BigDecimal.ZERO; }
  public void setTotalBonuses(BigDecimal totalBonuses) { this.totalBonuses = totalBonuses != null ? totalBonuses : BigDecimal.ZERO; }
  public BigDecimal getTotalDebt() { return totalDebt != null ? totalDebt : BigDecimal.ZERO; }
  public void setTotalDebt(BigDecimal totalDebt) { this.totalDebt = totalDebt != null ? totalDebt : BigDecimal.ZERO; }

  public static final class SafeBuilder {
    private LocalDate from;
    private LocalDate to;
    private long employeesPaid;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private BigDecimal totalDiscounts;
    private BigDecimal totalBonuses;
    private BigDecimal totalDebt;

    public SafeBuilder withFrom(LocalDate from) { this.from = from; return this; }
    public SafeBuilder withTo(LocalDate to) { this.to = to; return this; }
    public SafeBuilder withEmployeesPaid(long employeesPaid) { this.employeesPaid = employeesPaid; return this; }
    public SafeBuilder withTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; return this; }
    public SafeBuilder withTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; return this; }
    public SafeBuilder withTotalDiscounts(BigDecimal totalDiscounts) { this.totalDiscounts = totalDiscounts; return this; }
    public SafeBuilder withTotalBonuses(BigDecimal totalBonuses) { this.totalBonuses = totalBonuses; return this; }
    public SafeBuilder withTotalDebt(BigDecimal totalDebt) { this.totalDebt = totalDebt; return this; }

    public PayrollSummary build() {
      PayrollSummary summary = new PayrollSummary();
      summary.from = from;
      summary.to = to;
      summary.employeesPaid = employeesPaid;
      summary.totalGross = totalGross != null ? totalGross : BigDecimal.ZERO;
      summary.totalNet = totalNet != null ? totalNet : BigDecimal.ZERO;
      summary.totalDiscounts = totalDiscounts != null ? totalDiscounts : BigDecimal.ZERO;
      summary.totalBonuses = totalBonuses != null ? totalBonuses : BigDecimal.ZERO;
      summary.totalDebt = totalDebt != null ? totalDebt : BigDecimal.ZERO;
      return summary;
    }
  }
}
