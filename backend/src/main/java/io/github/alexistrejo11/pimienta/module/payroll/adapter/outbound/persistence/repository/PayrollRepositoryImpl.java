package io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.repository;

import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.PayrollPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollAdjustmentJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollDebtJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollPaymentJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollPeriodJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.outbound.persistence.model.PayrollRecordJpaEntity;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.query.PayrollSummaryQuery;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollDebt;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPayment;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPeriod;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollRecord;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollSummary;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollAdjustmentType;
import java.math.BigDecimal;
import io.github.alexistrejo11.pimienta.module.payroll.core.port.output.PayrollRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PayrollRepositoryImpl implements PayrollRepository {

  private final PayrollPeriodJpaRepository periodJpaRepository;
  private final PayrollRecordJpaRepository recordJpaRepository;
  private final PayrollPaymentJpaRepository paymentJpaRepository;
  private final PayrollDebtJpaRepository debtJpaRepository;
  private final PayrollAdjustmentJpaRepository adjustmentJpaRepository;
  private final PayrollPersistenceMapper mapper;

  public PayrollRepositoryImpl(
      PayrollPeriodJpaRepository periodJpaRepository,
      PayrollRecordJpaRepository recordJpaRepository,
      PayrollPaymentJpaRepository paymentJpaRepository,
      PayrollDebtJpaRepository debtJpaRepository,
      PayrollAdjustmentJpaRepository adjustmentJpaRepository,
      PayrollPersistenceMapper mapper) {
    this.periodJpaRepository = periodJpaRepository;
    this.recordJpaRepository = recordJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
    this.debtJpaRepository = debtJpaRepository;
    this.adjustmentJpaRepository = adjustmentJpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Page<PayrollPayment> findPayments(Long employeeId, Pageable pageable) {
    Page<PayrollPaymentJpaEntity> paymentEntities = paymentJpaRepository.searchByEmployee(employeeId, pageable);
    Page<PayrollPayment> payments = paymentEntities.map(mapper::toDomain);
    return payments;
  }

  @Override
  public Page<PayrollPeriod> findPeriods(Pageable pageable) {
    Page<PayrollPeriodJpaEntity> periodEntities = periodJpaRepository
        .findAllByDeletedAtIsNull(pageable);
    Page<PayrollPeriod> periods = periodEntities.map(mapper::toDomain);
    return periods;
  }

  @Override
  public Page<PayrollRecord> findRecords(Long employeeId, Long periodId, Pageable pageable) {
    Page<PayrollRecordJpaEntity> recordEntities = recordJpaRepository
        .search(employeeId, periodId, pageable);
    Page<PayrollRecord> records = recordEntities.map(mapper::toDomain);
    return records;
  }

  @Override
  public Page<PayrollDebt> findDebts(Pageable pageable) {
    Page<PayrollDebtJpaEntity> debtEntities = debtJpaRepository
        .findAllByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
    Page<PayrollDebt> debts = debtEntities.map(mapper::toDomain);
    return debts;
  }

  @Override
  public Optional<PayrollRecord> findRecordById(Long id) {
    Optional<PayrollRecordJpaEntity> recordEntity = recordJpaRepository
        .findByIdAndDeletedAtIsNull(id);
    Optional<PayrollRecord> record = recordEntity.map(mapper::toDomain);
    return record;
  }

  @Override
  public Optional<PayrollPayment> findLatestPaymentByRecordId(Long recordId) {
    if (recordId == null) {
      return Optional.empty();
    }
    Optional<PayrollPaymentJpaEntity> paymentEntity = paymentJpaRepository
        .findTopByPayrollRecordIdAndDeletedAtIsNullOrderByCreatedAtDesc(recordId);
    Optional<PayrollPayment> payment = paymentEntity.map(mapper::toDomain);
    return payment;
  }

  @Override
  @Transactional
  public PayrollPeriod savePeriod(PayrollPeriod period) {
    PayrollPeriodJpaEntity periodEntity = mapper
        .toEntity(period);
    PayrollPeriodJpaEntity savedPeriodEntity = periodJpaRepository
        .save(periodEntity);
    PayrollPeriod savedPeriod = mapper.toDomain(savedPeriodEntity);
    return savedPeriod;
  }

  @Override
  @Transactional
  public PayrollRecord saveRecord(PayrollRecord record) {
    PayrollRecordJpaEntity recordEntity = mapper
        .toEntity(record);
    PayrollRecordJpaEntity savedRecordEntity = recordJpaRepository
        .save(recordEntity);
    PayrollRecord savedRecord = mapper.toDomain(savedRecordEntity);
    return savedRecord;
  }

  @Override
  @Transactional
  public PayrollPayment savePayment(PayrollPayment payment) {
    PayrollPaymentJpaEntity paymentEntity = mapper.toEntity(payment);
    PayrollPaymentJpaEntity savedPaymentEntity = paymentJpaRepository.save(paymentEntity);
    PayrollPayment savedPayment = mapper.toDomain(savedPaymentEntity);
    return savedPayment;
  }

  @Override
  @Transactional
  public PayrollDebt saveDebt(PayrollDebt debt) {
    PayrollDebtJpaEntity debtEntity = mapper
        .toEntity(debt);
    PayrollDebtJpaEntity savedDebtEntity = debtJpaRepository
        .save(debtEntity);
    PayrollDebt savedDebt = mapper.toDomain(savedDebtEntity);
    return savedDebt;
  }

  @Override
  @Transactional
  public void appendAdjustment(Long recordId, PayrollAdjustmentType type, BigDecimal amount, String reason) {
    PayrollAdjustmentJpaEntity entity = new PayrollAdjustmentJpaEntity();
    entity.setPayrollRecordId(recordId);
    entity.setType(type);
    entity.setAmount(amount);
    entity.setReason(reason != null ? reason.trim() : "");
    entity.fillCreatedAndUpdatedIfNull();
    entity.fillUpdatedIfNull();
    adjustmentJpaRepository.save(entity);
  }

  @Override
  public PayrollSummary summarize(PayrollSummaryQuery query) {
    Object[] paymentSummary = paymentJpaRepository.summarizePayments(query.from(), query.to());
    Object[] adjustmentSummary = recordJpaRepository.summarizeAdjustments(query.from(), query.to());
    BigDecimal totalDebt = debtJpaRepository.summarizeOpenDebt();
    long employeesPaid = longAt(paymentSummary, 0);
    BigDecimal totalGross = decimalAt(paymentSummary, 1);
    BigDecimal totalNet = decimalAt(paymentSummary, 2);
    BigDecimal totalDiscounts = decimalAt(adjustmentSummary, 0);
    BigDecimal totalBonuses = decimalAt(adjustmentSummary, 1);
    BigDecimal normalizedTotalDebt = totalDebt != null ? totalDebt : BigDecimal.ZERO;
    PayrollSummary summary = PayrollSummary.builder()
        .withFrom(query.from())
        .withTo(query.to())
        .withEmployeesPaid(employeesPaid)
        .withTotalGross(totalGross)
        .withTotalNet(totalNet)
        .withTotalDiscounts(totalDiscounts)
        .withTotalBonuses(totalBonuses)
        .withTotalDebt(normalizedTotalDebt)
        .build();
    return summary;
  }

  private static BigDecimal decimalAt(Object[] row, int idx) {
    if (row == null || idx >= row.length || row[idx] == null) {
      return BigDecimal.ZERO;
    }
    Object value = row[idx];
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return BigDecimal.ZERO;
  }

  private static long longAt(Object[] row, int idx) {
    if (row == null || idx >= row.length || row[idx] == null) {
      return 0L;
    }
    Object value = row[idx];
    if (value instanceof Number number) {
      return number.longValue();
    }
    return 0L;
  }
}
