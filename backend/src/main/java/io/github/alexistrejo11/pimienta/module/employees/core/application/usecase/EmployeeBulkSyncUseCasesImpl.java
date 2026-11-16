package io.github.alexistrejo11.pimienta.module.employees.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.EmployeeExportRow;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.EmployeeImportRow;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.RegisterEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.UpdateEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.EmployeeSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeOnboardingPhase;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeManagementUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeSpreadsheetGenerator;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeSpreadsheetParser;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeRepository;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportRowError;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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
public class EmployeeBulkSyncUseCasesImpl implements EmployeeBulkSyncUseCases {

  private static final Logger log = LoggerFactory.getLogger(EmployeeBulkSyncUseCasesImpl.class);

  private static final int PAGE_SIZE = 500;

  private final EmployeeRepository employeeRepository;
  private final EmployeeManagementUseCases employeeManagementUseCases;
  private final EmployeeSpreadsheetParser spreadsheetParser;
  private final EmployeeSpreadsheetGenerator spreadsheetGenerator;

  public EmployeeBulkSyncUseCasesImpl(
      EmployeeRepository employeeRepository,
      EmployeeManagementUseCases employeeManagementUseCases,
      EmployeeSpreadsheetParser spreadsheetParser,
      EmployeeSpreadsheetGenerator spreadsheetGenerator) {
    this.employeeRepository = employeeRepository;
    this.employeeManagementUseCases = employeeManagementUseCases;
    this.spreadsheetParser = spreadsheetParser;
    this.spreadsheetGenerator = spreadsheetGenerator;
  }

  @Override
  public byte[] exportEmployees(EmployeeSearchCriteria criteria, Pageable pageable) throws IOException {
    EmployeeSearchCriteria effective = criteria != null ? criteria : EmployeeSearchCriteria.empty();

    log.debug(
        "export employees start status={} department={} textLen={}",
        effective.status(),
        effective.department(),
        effective.text() != null ? effective.text().length() : 0);

    List<EmployeeExportRow> rows = new ArrayList<>();
    int page = 0;
    Sort sort = pageable != null && pageable.getSort().isSorted()
        ? pageable.getSort()
        : Sort.by(Sort.Direction.ASC, "id");

    for (;;) {
      Page<Employee> p = employeeRepository.search(effective, PageRequest.of(page, PAGE_SIZE, sort));

      for (Employee e : p.getContent()) {
        rows.add(toExportRow(e));
      }
      if (!p.hasNext()) {
        break;
      }
      page++;
    }

    byte[] bytes = spreadsheetGenerator.generate(rows);

    log.debug("export employees complete rowCount={} bytesLen={}", rows.size(), bytes.length);
    return bytes;
  }

  @Override
  @Transactional
  public SpreadsheetBulkImportResult importEmployees(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException {
    log.info(
        "import employees start dryRun={} originalFilename={}",
        dryRun,
        originalFilename != null ? originalFilename : "null");

    List<EmployeeImportRow> parsed = spreadsheetParser.parse(file, originalFilename);
    List<SpreadsheetBulkImportRowError> errors = new ArrayList<>();
    List<PlannedEmployeeRow> planned = new ArrayList<>();
    int skipped = 0;

    for (EmployeeImportRow row : parsed) {
      if (shouldSkipRowQuietly(row)) {
        skipped++;
        continue;
      }
      try {
        planned.add(planRow(row));
      } catch (Exception ex) {
        errors.add(rowError(row, ex.getMessage() != null ? ex.getMessage() : "Error al procesar fila"));
      }
    }

    if (!errors.isEmpty()) {
      return new SpreadsheetBulkImportResult(0, 0, skipped, List.copyOf(errors), dryRun);
    }

    int updated = 0;
    int created = 0;
    for (PlannedEmployeeRow p : planned) {
      if (p.update()) {
        updated++;
      } else {
        created++;
      }
    }

    if (!dryRun) {
      for (PlannedEmployeeRow p : planned) {
        applyPlanned(p);
      }
    }

    log.info(
        "import employees complete dryRun={} updated={} created={} skipped={}",
        dryRun,
        updated,
        created,
        skipped);
    return new SpreadsheetBulkImportResult(updated, created, skipped, List.of(), dryRun);
  }

  private static EmployeeExportRow toExportRow(Employee e) {
    var personal = e.getPersonal();
    var ids = e.getOfficialIds();
    var employment = e.getEmployment();
    var pay = e.getCompensation();
    var benefits = e.getBenefits();
    return new EmployeeExportRow(
        e.getId(),
        personal.firstName(),
        personal.lastName(),
        personal.email(),
        personal.phone(),
        personal.address(),
        ids.curp(),
        ids.rfc(),
        ids.nss(),
        ids.clabe(),
        employment.position(),
        employment.department(),
        e.getStatus(),
        employment.contractType().name(),
        employment.workShift().name(),
        pay.salaryPerWeek() != null ? pay.salaryPerWeek().toPlainString() : "",
        pay.bonuses() != null ? pay.bonuses().toPlainString() : "",
        pay.foodVouchers() != null ? pay.foodVouchers().toPlainString() : "",
        benefits.integrationFactor() != null ? benefits.integrationFactor().toPlainString() : "");
  }

  private PlannedEmployeeRow planRow(EmployeeImportRow row) {
    if (row.id() != null) {
      Employee existing = employeeRepository.findById(row.id()).orElse(null);
      if (existing == null) {
        throw new IllegalArgumentException("No existe empleado con ID " + row.id());
      }
      ContractType contractType =
          parseEnum(ContractType.class, row.contractTypeRaw(), existing.getEmployment().contractType());
      WorkShift workShift =
          parseEnum(WorkShift.class, row.workShiftRaw(), existing.getEmployment().workShift());
      EmployeeStatus statusParsed = parseStatus(row.statusRaw());
      UpdateEmployeeParams merged = mergeUpdate(row, existing, contractType, workShift);
      return PlannedEmployeeRow.update(row, merged, statusParsed);
    }
    if (row.firstName() == null || row.firstName().isBlank()) {
      throw new IllegalArgumentException("El nombre es obligatorio para altas");
    }
    if (row.lastName() == null || row.lastName().isBlank()) {
      throw new IllegalArgumentException("El apellido es obligatorio para altas");
    }
    ContractType contractType =
        parseEnum(ContractType.class, row.contractTypeRaw(), ContractType.UNDEFINED);
    WorkShift workShift = parseEnum(WorkShift.class, row.workShiftRaw(), WorkShift.UNDEFINED);
    EmployeeStatus statusParsed = parseStatus(row.statusRaw());
    RegisterEmployeeParams reg = buildRegisterParamsForBulk(row, contractType, workShift);
    return PlannedEmployeeRow.create(row, reg, statusParsed);
  }

  private void applyPlanned(PlannedEmployeeRow planned) {
    if (planned.updateParams() != null) {
      employeeManagementUseCases.update(planned.row().id(), planned.updateParams());
      if (planned.status() != null) {
        Employee after = employeeManagementUseCases.getById(planned.row().id());
        persistStatusIfChanged(after, planned.status());
      }
      return;
    }
    Employee createdEmp = employeeManagementUseCases.register(planned.registerParams());
    persistStatusIfChanged(createdEmp, planned.status());
  }

  private static boolean shouldSkipRowQuietly(EmployeeImportRow row) {
    return row.id() == null && (row.firstName() == null || row.firstName().isBlank());
  }

  private void persistStatusIfChanged(Employee employee, EmployeeStatus desired) {
    if (desired == null || desired.equals(employee.getStatus())) {
      return;
    }
    employee.setStatus(desired);
    employeeRepository.save(employee);
  }

  private static RegisterEmployeeParams buildRegisterParamsForBulk(
      EmployeeImportRow row, ContractType contractType, WorkShift workShift) {
    String email = row.email() != null && !row.email().isBlank() ? row.email().trim() : null;
    return new RegisterEmployeeParams(
        row.firstName().trim(),
        row.lastName().trim(),
        email,
        nz(row.phone(), ""),
        nz(row.address(), ""),
        nz(row.curp(), ""),
        nz(row.rfc(), ""),
        nz(row.nss(), ""),
        nz(row.clabe(), ""),
        "",
        nz(row.position(), ""),
        nz(row.department(), ""),
        contractType,
        workShift,
        row.salaryPerWeek() != null ? row.salaryPerWeek() : BigDecimal.ZERO,
        null,
        EmployeeOnboardingPhase.DRAFT,
        null);
  }

  private static SpreadsheetBulkImportRowError rowError(EmployeeImportRow row, String message) {
    return new SpreadsheetBulkImportRowError(row.excelRowNumber(), message);
  }

  private static UpdateEmployeeParams mergeUpdate(
      EmployeeImportRow row,
      Employee e,
      ContractType contractType,
      WorkShift workShift) {
    var pay = e.getCompensation();
    var benefits = e.getBenefits();
    BigDecimal salary = row.salaryPerWeek() != null ? row.salaryPerWeek() : pay.salaryPerWeek();
    BigDecimal bonuses = row.bonuses() != null ? row.bonuses() : pay.bonuses();
    BigDecimal food = row.foodVouchers() != null ? row.foodVouchers() : pay.foodVouchers();
    BigDecimal integration = row.integrationFactor() != null ? row.integrationFactor() : benefits.integrationFactor();
    return new UpdateEmployeeParams(
        row.id(),
        nz(row.firstName(), e.getPersonal().firstName()),
        nz(row.lastName(), e.getPersonal().lastName()),
        null,
        nz(row.email(), e.getPersonal().email()),
        nz(row.phone(), e.getPersonal().phone()),
        nz(row.address(), e.getPersonal().address()),
        nz(row.curp(), e.getOfficialIds().curp()),
        nz(row.rfc(), e.getOfficialIds().rfc()),
        nz(row.nss(), e.getOfficialIds().nss()),
        nz(row.clabe(), e.getOfficialIds().clabe()),
        nz(row.position(), e.getEmployment().position()),
        nz(row.department(), e.getEmployment().department()),
        contractType,
        workShift,
        salary,
        bonuses,
        food,
        integration);
  }

  private static String nz(String fromRow, String existing) {
    if (fromRow == null || fromRow.isBlank()) {
      return existing != null ? existing : "";
    }
    return fromRow.trim();
  }

  private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E defaultValue) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    String s = raw.trim()
        .toUpperCase(Locale.ROOT)
        .replace(' ', '_')
        .replace('-', '_');
    return Enum.valueOf(type, s);
  }

  private static EmployeeStatus parseStatus(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String s = raw.trim()
        .toUpperCase(Locale.ROOT)
        .replace(' ', '_')
        .replace('-', '_');
    return EmployeeStatus.valueOf(s);
  }

  private static final class PlannedEmployeeRow {
    private final EmployeeImportRow row;
    private final UpdateEmployeeParams updateParams;
    private final RegisterEmployeeParams registerParams;
    private final EmployeeStatus status;

    private PlannedEmployeeRow(
        EmployeeImportRow row,
        UpdateEmployeeParams updateParams,
        RegisterEmployeeParams registerParams,
        EmployeeStatus status) {
      this.row = row;
      this.updateParams = updateParams;
      this.registerParams = registerParams;
      this.status = status;
    }

    static PlannedEmployeeRow update(
        EmployeeImportRow row, UpdateEmployeeParams updateParams, EmployeeStatus status) {
      return new PlannedEmployeeRow(row, updateParams, null, status);
    }

    static PlannedEmployeeRow create(
        EmployeeImportRow row, RegisterEmployeeParams registerParams, EmployeeStatus status) {
      return new PlannedEmployeeRow(row, null, registerParams, status);
    }

    boolean update() {
      return updateParams != null;
    }

    EmployeeImportRow row() {
      return row;
    }

    UpdateEmployeeParams updateParams() {
      return updateParams;
    }

    RegisterEmployeeParams registerParams() {
      return registerParams;
    }

    EmployeeStatus status() {
      return status;
    }
  }
}
