package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
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
import java.time.LocalDate;

@Entity
@Table(
    name = "payroll_periods",
    indexes = {
      @Index(name = "idx_payroll_periods_dates", columnList = "start_date, end_date"),
      @Index(name = "idx_payroll_periods_status", columnList = "status"),
      @Index(name = "idx_payroll_periods_deleted_at", columnList = "deleted_at")
    })
public class PayrollPeriodJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PayrollFrequency frequency;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(nullable = false, length = 32)
  private String status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PayrollFrequency getFrequency() {
    return frequency != null ? frequency : PayrollFrequency.CUSTOM;
  }

  public void setFrequency(PayrollFrequency frequency) {
    this.frequency = frequency;
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
}
