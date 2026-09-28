package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollDebtJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollPaymentJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollPeriodJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollRecordJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollDebt;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPayment;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPeriod;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PayrollPersistenceMapper {

  public PayrollPeriod toDomain(PayrollPeriodJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    PayrollPeriod period = PayrollPeriod.builder()
        .withId(entity.getId())
        .withFrequency(entity.getFrequency())
        .withStartDate(entity.getStartDate())
        .withEndDate(entity.getEndDate())
        .withStatus(entity.getStatus())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
    return period;
  }

  public PayrollPeriodJpaEntity toEntity(PayrollPeriod domain) {
    PayrollPeriodJpaEntity entity = new PayrollPeriodJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setFrequency(domain.getFrequency());
    entity.setStartDate(domain.getStartDate());
    entity.setEndDate(domain.getEndDate());
    entity.setStatus(text(domain.getStatus(), "OPEN"));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    entity.fillCreatedAndUpdatedIfNull();
    entity.fillUpdatedIfNull();
    return entity;
  }

  public PayrollRecord toDomain(PayrollRecordJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    PayrollRecord record = PayrollRecord.builder()
        .withId(entity.getId())
        .withEmployeeId(entity.getEmployeeId())
        .withPeriodId(entity.getPeriodId())
        .withWorkedDaysStart(entity.getWorkedDaysStart())
        .withWorkedDaysEnd(entity.getWorkedDaysEnd())
        .withGrossAmount(entity.getGrossAmount())
        .withTotalDiscounts(entity.getTotalDiscounts())
        .withTotalBonuses(entity.getTotalBonuses())
        .withNetAmount(entity.getNetAmount())
        .withStatus(entity.getStatus())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
    return record;
  }

  public PayrollRecordJpaEntity toEntity(PayrollRecord domain) {
    PayrollRecordJpaEntity entity = new PayrollRecordJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setEmployeeId(domain.getEmployeeId());
    entity.setPeriodId(domain.getPeriodId());
    entity.setWorkedDaysStart(domain.getWorkedDaysStart());
    entity.setWorkedDaysEnd(domain.getWorkedDaysEnd());
    entity.setGrossAmount(nvl(domain.getGrossAmount()));
    entity.setTotalDiscounts(nvl(domain.getTotalDiscounts()));
    entity.setTotalBonuses(nvl(domain.getTotalBonuses()));
    entity.setNetAmount(nvl(domain.getNetAmount()));
    entity.setStatus(domain.getStatus());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    entity.fillCreatedAndUpdatedIfNull();
    entity.fillUpdatedIfNull();
    return entity;
  }

  public PayrollPayment toDomain(PayrollPaymentJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PayrollPayment.builder()
        .withId(entity.getId())
        .withPayrollRecordId(entity.getPayrollRecordId())
        .withEmployeeId(entity.getEmployeeId())
        .withFrequency(entity.getFrequency())
        .withWorkedDaysStart(entity.getWorkedDaysStart())
        .withWorkedDaysEnd(entity.getWorkedDaysEnd())
        .withGrossAmount(entity.getGrossAmount())
        .withNetAmount(entity.getNetAmount())
        .withDestinationAccount(entity.getDestinationAccount())
        .withTransactionId(entity.getTransactionId())
        .withStatus(entity.getStatus())
        .withPendingAmount(entity.getPendingAmount())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public PayrollPaymentJpaEntity toEntity(PayrollPayment domain) {
    PayrollPaymentJpaEntity entity = new PayrollPaymentJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setPayrollRecordId(domain.getPayrollRecordId());
    entity.setEmployeeId(domain.getEmployeeId());
    entity.setFrequency(domain.getFrequency());
    entity.setWorkedDaysStart(domain.getWorkedDaysStart());
    entity.setWorkedDaysEnd(domain.getWorkedDaysEnd());
    entity.setGrossAmount(nvl(domain.getGrossAmount()));
    entity.setNetAmount(nvl(domain.getNetAmount()));
    entity.setDestinationAccount(text(domain.getDestinationAccount(), ""));
    entity.setTransactionId(text(domain.getTransactionId(), ""));
    entity.setStatus(domain.getStatus());
    entity.setPendingAmount(nvl(domain.getPendingAmount()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    entity.fillCreatedAndUpdatedIfNull();
    entity.fillUpdatedIfNull();
    return entity;
  }

  public PayrollDebt toDomain(PayrollDebtJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    PayrollDebt debt = PayrollDebt.builder()
        .withId(entity.getId())
        .withEmployeeId(entity.getEmployeeId())
        .withPayrollRecordId(entity.getPayrollRecordId())
        .withAmountOwed(entity.getAmountOwed())
        .withReason(entity.getReason())
        .withSettled(entity.isSettled())
        .withSettledAt(entity.getSettledAt())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
    return debt;
  }

  public PayrollDebtJpaEntity toEntity(PayrollDebt domain) {
    PayrollDebtJpaEntity entity = new PayrollDebtJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setEmployeeId(domain.getEmployeeId());
    entity.setPayrollRecordId(domain.getPayrollRecordId());
    entity.setAmountOwed(nvl(domain.getAmountOwed()));
    entity.setReason(domain.getReason());
    entity.setSettled(domain.isSettled());
    entity.setSettledAt(domain.getSettledAt());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    entity.fillCreatedAndUpdatedIfNull();
    entity.fillUpdatedIfNull();
    return entity;
  }

  private static BigDecimal nvl(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String text(String value, String fallback) {
    if (value == null) {
      return fallback;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? fallback : trimmed;
  }

}
