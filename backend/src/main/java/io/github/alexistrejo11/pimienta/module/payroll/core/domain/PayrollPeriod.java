package io.github.alexistrejo11.pimienta.module.payroll.core.domain;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollPeriod extends BaseDomain<Long> {

  private PayrollFrequency frequency;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;

  private PayrollPeriod() {
    this.id = 0L;
    this.frequency = PayrollFrequency.CUSTOM;
    this.status = "OPEN";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public PayrollFrequency getFrequency() {
    return frequency != null ? frequency : PayrollFrequency.CUSTOM;
  }

  public void setFrequency(PayrollFrequency frequency) {
    this.frequency = frequency != null ? frequency : PayrollFrequency.CUSTOM;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getStatus() {
    return status != null ? status : "";
  }

  public void setStatus(String status) {
    this.status = status != null ? status.trim() : "";
  }

  public static final class SafeBuilder {
    private Long id;
    private PayrollFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withFrequency(PayrollFrequency frequency) {
      this.frequency = frequency;
      return this;
    }

    public SafeBuilder withStartDate(LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public SafeBuilder withEndDate(LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public SafeBuilder withStatus(String status) {
      this.status = status != null ? status.trim() : "";
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public PayrollPeriod reconstruct() {
      PayrollPeriod period = new PayrollPeriod();
      period.id = id != null ? id : 0L;
      period.frequency = frequency != null ? frequency : PayrollFrequency.CUSTOM;
      period.startDate = startDate;
      period.endDate = endDate;
      period.status = status != null ? status : "OPEN";
      period.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      period.updatedAt = updatedAt != null ? updatedAt : period.createdAt;
      period.deletedAt = deletedAt;
      period.version = version;
      return period;
    }

    public PayrollPeriod register() {
      return reconstruct();
    }
  }
}
