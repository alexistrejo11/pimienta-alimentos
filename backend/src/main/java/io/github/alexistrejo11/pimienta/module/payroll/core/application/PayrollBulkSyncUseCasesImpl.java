package io.github.alexistrejo11.pimienta.module.payroll.core.application;

import io.github.alexistrejo11.pimienta.module.payroll.core.application.dto.PayrollExportRow;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.dto.PayrollImportRow;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.query.PayrollBulkScopeQuery;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollRecord;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollRecordStatus;
import io.github.alexistrejo11.pimienta.module.payroll.core.port.input.PayrollSpreadsheetGenerator;
import io.github.alexistrejo11.pimienta.module.payroll.core.port.input.PayrollSpreadsheetParser;
import io.github.alexistrejo11.pimienta.module.payroll.core.port.output.PayrollRepository;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportRowError;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollBulkSyncUseCasesImpl implements PayrollBulkSyncUseCases {

  private static final Logger log = LoggerFactory.getLogger(PayrollBulkSyncUseCasesImpl.class);

  private static final int PAGE_SIZE = 500;

  private final PayrollRepository payrollRepository;
  private final PayrollSpreadsheetParser spreadsheetParser;
  private final PayrollSpreadsheetGenerator spreadsheetGenerator;

  public PayrollBulkSyncUseCasesImpl(
      PayrollRepository payrollRepository,
      PayrollSpreadsheetParser spreadsheetParser,
      PayrollSpreadsheetGenerator spreadsheetGenerator) {
    this.payrollRepository = payrollRepository;
    this.spreadsheetParser = spreadsheetParser;
    this.spreadsheetGenerator = spreadsheetGenerator;
  }

  @Override
  public byte[] exportPayrollRecords(PayrollBulkScopeQuery scope, Pageable pageable) throws IOException {
    PayrollBulkScopeQuery effective = scope != null ? scope : new PayrollBulkScopeQuery(null, null, null, null);

    log.debug(
        "export payroll records start employeeId={} periodId={} workedFrom={} workedTo={}",
        effective.employeeId(),
        effective.periodId(),
        effective.workedFrom(),
        effective.workedTo());

    List<PayrollExportRow> rows = new ArrayList<>();
    Sort sort =
        pageable != null && pageable.getSort().isSorted()
            ? pageable.getSort()
            : Sort.by(Sort.Direction.ASC, "id");
    int page = 0;

    for (;;) {
      Page<PayrollRecord> records =
          payrollRepository.findRecords(
              effective.employeeId(),
              effective.periodId(),
              PageRequest.of(page, PAGE_SIZE, sort));

      for (PayrollRecord record : records.getContent()) {
        if (!inScope(record.getWorkedDaysStart(), record.getWorkedDaysEnd(), effective)) {
          continue;
        }
        rows.add(
            new PayrollExportRow(
                record.getId(),
                record.getEmployeeId(),
                record.getPeriodId(),
                record.getWorkedDaysStart(),
                record.getWorkedDaysEnd(),
                record.getGrossAmount(),
                record.getTotalDiscounts(),
                record.getTotalBonuses(),
                record.getNetAmount(),
                record.getStatus()));
      }
      if (!records.hasNext()) {
        break;
      }
      page++;
    }

    byte[] bytes = spreadsheetGenerator.generate(rows);

    log.debug("export payroll records complete rowCount={} bytesLen={}", rows.size(), bytes.length);
    return bytes;
  }

  @Override
  @Transactional
  public SpreadsheetBulkImportResult importPayrollRecords(
      InputStream file, String originalFilename, PayrollBulkScopeQuery scope, boolean dryRun)
      throws IOException {
    PayrollBulkScopeQuery effective = scope != null ? scope : new PayrollBulkScopeQuery(null, null, null, null);

    log.info(
        "import payroll records start dryRun={} originalFilename={}",
        dryRun,
        originalFilename != null ? originalFilename : "null");

    List<PayrollImportRow> parsedRows = spreadsheetParser.parse(file, originalFilename);
    List<SpreadsheetBulkImportRowError> errors = new ArrayList<>();
    List<PayrollRecord> toSave = new ArrayList<>();
    int skipped = 0;
    int updated = 0;
    int created = 0;

    for (PayrollImportRow row : parsedRows) {
      try {
        if (isQuietSkip(row)) {
          skipped++;
          continue;
        }
        if (!inScope(row.workedDaysStart(), row.workedDaysEnd(), effective)) {
          skipped++;
          continue;
        }
        if (row.id() != null) {
          PayrollRecord existing =
              payrollRepository
                  .findRecordById(row.id())
                  .orElseThrow(() -> new IllegalArgumentException("No existe payroll record con id " + row.id()));
          toSave.add(merge(existing, row));
          updated++;
          continue;
        }
        if (row.employeeId() == null
            || row.workedDaysStart() == null
            || row.workedDaysEnd() == null
            || row.grossAmount() == null) {
          throw new IllegalArgumentException(
              "Para crear se requiere employee_id, worked_days_start, worked_days_end y gross_amount");
        }
        PayrollRecord createdRecord =
            PayrollRecord.builder()
                .withEmployeeId(row.employeeId())
                .withPeriodId(row.periodId())
                .withWorkedDaysStart(row.workedDaysStart())
                .withWorkedDaysEnd(row.workedDaysEnd())
                .withGrossAmount(nz(row.grossAmount()))
                .withTotalDiscounts(nz(row.totalDiscounts()))
                .withTotalBonuses(nz(row.totalBonuses()))
                .withNetAmount(nz(row.netAmount(), row.grossAmount()))
                .withStatus(parseStatus(row.statusRaw(), PayrollRecordStatus.PENDING))
                .register();
        toSave.add(createdRecord);
        created++;
      } catch (Exception ex) {
        errors.add(
            new SpreadsheetBulkImportRowError(
                row.excelRowNumber(),
                ex.getMessage() != null ? ex.getMessage() : "Error al procesar fila"));
      }
    }

    if (!errors.isEmpty()) {
      return new SpreadsheetBulkImportResult(0, 0, skipped, List.copyOf(errors), dryRun);
    }
    if (!dryRun) {
      for (PayrollRecord record : toSave) {
        payrollRepository.saveRecord(record);
      }
    }
    return new SpreadsheetBulkImportResult(updated, created, skipped, List.of(), dryRun);
  }

  private static boolean isQuietSkip(PayrollImportRow row) {
    return row.id() == null
        && row.employeeId() == null
        && row.workedDaysStart() == null
        && row.workedDaysEnd() == null
        && row.grossAmount() == null;
  }

  private static PayrollRecord merge(PayrollRecord existing, PayrollImportRow row) {
    Long employeeId = row.employeeId() != null ? row.employeeId() : existing.getEmployeeId();
    Long periodId = row.periodId() != null ? row.periodId() : existing.getPeriodId();
    LocalDate workedStart = row.workedDaysStart() != null ? row.workedDaysStart() : existing.getWorkedDaysStart();
    LocalDate workedEnd = row.workedDaysEnd() != null ? row.workedDaysEnd() : existing.getWorkedDaysEnd();
    BigDecimal gross = row.grossAmount() != null ? row.grossAmount() : existing.getGrossAmount();
    BigDecimal discounts = row.totalDiscounts() != null ? row.totalDiscounts() : existing.getTotalDiscounts();
    BigDecimal bonuses = row.totalBonuses() != null ? row.totalBonuses() : existing.getTotalBonuses();
    BigDecimal net = row.netAmount() != null ? row.netAmount() : existing.getNetAmount();
    PayrollRecordStatus status = parseStatus(row.statusRaw(), existing.getStatus());
    PayrollRecord merged = PayrollRecord.builder()
        .withId(existing.getId())
        .withEmployeeId(employeeId)
        .withPeriodId(periodId)
        .withWorkedDaysStart(workedStart)
        .withWorkedDaysEnd(workedEnd)
        .withGrossAmount(nz(gross))
        .withTotalDiscounts(nz(discounts))
        .withTotalBonuses(nz(bonuses))
        .withNetAmount(nz(net, gross))
        .withStatus(status)
        .withCreatedAt(existing.getCreatedAt())
        .withUpdatedAt(LocalDate.now().atStartOfDay())
        .withDeletedAt(existing.getDeletedAt())
        .withVersion(existing.getVersion())
        .reconstruct();
    return merged;
  }

  private static boolean inScope(LocalDate start, LocalDate end, PayrollBulkScopeQuery scope) {
    if (scope == null) {
      return true;
    }
    if (scope.workedFrom() != null) {
      if (start == null || start.isBefore(scope.workedFrom())) {
        return false;
      }
    }
    if (scope.workedTo() != null) {
      if (end == null || end.isAfter(scope.workedTo())) {
        return false;
      }
    }
    return true;
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static BigDecimal nz(BigDecimal value, BigDecimal fallback) {
    if (value != null) {
      return value;
    }
    return fallback != null ? fallback : BigDecimal.ZERO;
  }

  private static PayrollRecordStatus parseStatus(String raw, PayrollRecordStatus fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback != null ? fallback : PayrollRecordStatus.PENDING;
    }
    String normalized = raw.trim()
        .toUpperCase(Locale.ROOT)
        .replace(' ', '_')
        .replace('-', '_');
    return PayrollRecordStatus.valueOf(normalized);
  }
}
