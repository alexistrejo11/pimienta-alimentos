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
import java.util.Optional;
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
    return EmployeeExportRow.builder()
        .id(e.getId())
        .firstName(personal.firstName())
        .lastName(personal.lastName())
        .email(personal.email())
        .phone(personal.phone())
        .address(personal.address())
        .curp(ids.curp())
        .rfc(ids.rfc())
        .nss(ids.nss())
        .clabe(ids.clabe())
        .position(employment.position())
        .department(employment.department())
        .status(e.getStatus())
        .contractType(employment.contractType().name())
        .workShift(employment.workShift().name())
        .salaryPerWeek(pay.salaryPerWeek() != null ? pay.salaryPerWeek().toPlainString() : "")
        .bonuses(pay.bonuses() != null ? pay.bonuses().toPlainString() : "")
        .foodVouchers(pay.foodVouchers() != null ? pay.foodVouchers().toPlainString() : "")
        .integrationFactor(
            benefits.integrationFactor() != null ? benefits.integrationFactor().toPlainString() : "")
        .build();
  }

  private PlannedEmployeeRow planRow(EmployeeImportRow row) {
    Optional<String> emailMissing = missingEmailForNewEmployee(row);
    if (emailMissing.isPresent()) {
      throw new IllegalArgumentException(emailMissing.get());
    }
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
    ContractType contractType = parseEnumRequired(ContractType.class, row.contractTypeRaw(), "ContractType");
    WorkShift workShift = parseEnumRequired(WorkShift.class, row.workShiftRaw(), "WorkShift");
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

  /** When there is no employee id, email is required. */
  private static Optional<String> missingEmailForNewEmployee(EmployeeImportRow row) {
    if (row.id() != null) {
      return Optional.empty();
    }
    if (row.email() == null || row.email().isBlank()) {
      return Optional.of("Email obligatorio para altas sin ID");
    }
    return Optional.empty();
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
    return RegisterEmployeeParams.builder()
        .firstName(row.firstName().trim())
        .lastName(row.lastName().trim())
        .email(row.email().trim())
        .phone(nz(row.phone(), ""))
        .address(nz(row.address(), ""))
        .curp(nz(row.curp(), ""))
        .rfc(nz(row.rfc(), ""))
        .nss(nz(row.nss(), ""))
        .clabe(nz(row.clabe(), ""))
        .employeeNumber("")
        .position(nz(row.position(), ""))
        .department(nz(row.department(), ""))
        .contractType(contractType)
        .workShift(workShift)
        .salaryPerWeek(row.salaryPerWeek() != null ? row.salaryPerWeek() : BigDecimal.ZERO)
        .birthDate(null)
        .onboardingPhase(EmployeeOnboardingPhase.DRAFT)
        .photo(null)
        .build();
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
    return UpdateEmployeeParams.builder()
        .id(row.id())
        .firstName(nz(row.firstName(), e.getPersonal().firstName()))
        .lastName(nz(row.lastName(), e.getPersonal().lastName()))
        .email(nz(row.email(), e.getPersonal().email()))
        .phone(nz(row.phone(), e.getPersonal().phone()))
        .address(nz(row.address(), e.getPersonal().address()))
        .curp(nz(row.curp(), e.getOfficialIds().curp()))
        .rfc(nz(row.rfc(), e.getOfficialIds().rfc()))
        .nss(nz(row.nss(), e.getOfficialIds().nss()))
        .clabe(nz(row.clabe(), e.getOfficialIds().clabe()))
        .position(nz(row.position(), e.getEmployment().position()))
        .department(nz(row.department(), e.getEmployment().department()))
        .contractType(contractType)
        .workShift(workShift)
        .salaryPerWeek(salary)
        .bonuses(bonuses)
        .foodVouchers(food)
        .integrationFactor(integration)
        .build();
  }

  private static String nz(String fromRow, String existing) {
    if (fromRow == null || fromRow.isBlank()) {
      return existing != null ? existing : "";
    }
    return fromRow.trim();
  }

  private static <E extends Enum<E>> E parseEnumRequired(Class<E> type, String raw, String field) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException(field + " es obligatorio para altas");
    }
    return parseEnum(type, raw, null);
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
