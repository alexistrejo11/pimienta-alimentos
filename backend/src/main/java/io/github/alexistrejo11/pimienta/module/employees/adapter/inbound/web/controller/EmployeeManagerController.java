package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.controller;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.RegisterEmployeeMultipartPayloadReader;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.UpdateEmployeeMultipartPayloadReader;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.RegisterEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.EmployeeSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeStatistics;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeManagementUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeSummary;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.EmployeePhotoUrlPresenter;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeDelete;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeExport;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeGetById;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeImport;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeListActive;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeRegister;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeRegisterJsonHidden;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeRehire;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeSearch;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeStatistics;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeSubmitForContract;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeSummary;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeTerminate;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeUpdate;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeUpdateJsonHidden;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployees;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeListItemResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeStatisticsResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeSummaryResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.RegisterEmployeeRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.UpdateEmployeeRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.EmployeeManagerWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/employees")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocEmployees
public class EmployeeManagerController {

  private final EmployeeManagementUseCases employeeManagementUseCases;
  private final EmployeeBulkSyncUseCases employeeBulkSyncUseCases;
  private final EmployeePhotoUrlPresenter employeePhotoUrlPresenter;
  private final RegisterEmployeeMultipartPayloadReader registerEmployeeMultipartPayloadReader;
  private final UpdateEmployeeMultipartPayloadReader updateEmployeeMultipartPayloadReader;

  public EmployeeManagerController(
      EmployeeManagementUseCases employeeManagementUseCases,
      EmployeeBulkSyncUseCases employeeBulkSyncUseCases,
      EmployeePhotoUrlPresenter employeePhotoUrlPresenter,
      RegisterEmployeeMultipartPayloadReader registerEmployeeMultipartPayloadReader,
      UpdateEmployeeMultipartPayloadReader updateEmployeeMultipartPayloadReader) {
    this.employeeManagementUseCases = employeeManagementUseCases;
    this.employeeBulkSyncUseCases = employeeBulkSyncUseCases;
    this.employeePhotoUrlPresenter = employeePhotoUrlPresenter;
    this.registerEmployeeMultipartPayloadReader = registerEmployeeMultipartPayloadReader;
    this.updateEmployeeMultipartPayloadReader = updateEmployeeMultipartPayloadReader;
  }

  @GetMapping("/statistics")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeStatistics
  public EmployeeStatisticsResponse getStatistics() {
    EmployeeStatistics statistics = employeeManagementUseCases.statistics();
    return EmployeeManagerWebMapper.toStatisticsResponse(statistics);
  }

  @GetMapping("/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeSummary
  public EmployeeSummaryResponse getSummary() {
    EmployeeSummary summary = employeeManagementUseCases.summary();
    return EmployeeManagerWebMapper.toSummaryResponse(summary);
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeSearch
  public PagedResponse<EmployeeListItemResponse> searchEmployees(
      @RequestParam(required = false) EmployeeStatus status,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String q,
      @ParameterObject @ModelAttribute PageableRequest pageable) {
    EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(status, blankToNull(department), blankToNull(q));
    Page<Employee> employees = employeeManagementUseCases.search(criteria, pageable.toPageable());
    return PagedResponse.map(
        employees, e -> EmployeeManagerWebMapper.toListItem(e, employeePhotoUrlPresenter::present));
  }

  @GetMapping("/active")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeListActive
  public PagedResponse<EmployeeListItemResponse> getActiveEmployees(@ParameterObject @ModelAttribute PageableRequest pageable) {
    Page<Employee> employees = employeeManagementUseCases.search(
        EmployeeSearchCriteria.onlyActive(), pageable.toPageable());
    return PagedResponse.map(
        employees, e -> EmployeeManagerWebMapper.toListItem(e, employeePhotoUrlPresenter::present));
  }

  @GetMapping("/export")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeExport
  public ResponseEntity<byte[]> exportEmployees(
      @RequestParam(required = false) EmployeeStatus status,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String q,
      @ParameterObject @ModelAttribute PageableRequest pageable)
      throws IOException {
    EmployeeSearchCriteria criteria = new EmployeeSearchCriteria(status, blankToNull(department), blankToNull(q));
    byte[] bytes = employeeBulkSyncUseCases.exportEmployees(criteria, pageable.toPageable());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empleados_reporte.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeImport
  public SpreadsheetBulkImportResult importEmployees(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun)
      throws IOException {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    return employeeBulkSyncUseCases.importEmployees(
        file.getInputStream(), file.getOriginalFilename(), dryRun);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocEmployeeRegister
  public EmployeeResponse registerEmployee(
      @RequestPart("employee") MultipartFile employeePayload,
      @RequestPart(value = "photo", required = false) MultipartFile photo) throws MethodArgumentNotValidException {
    RegisterEmployeeRequest parsed = registerEmployeeMultipartPayloadReader.readAndValidate(employeePayload);
    return registerEmployeeInternal(parsed, photo);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocEmployeeRegisterJsonHidden
  public EmployeeResponse registerEmployeeJsonOnly(@Valid @RequestBody RegisterEmployeeRequest request) {
    return registerEmployeeInternal(request, null);
  }

  @PutMapping("/{id}/submit-for-contract")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeSubmitForContract
  public EmployeeResponse submitEmployeeForContract(@PathVariable Long id) {
    Employee employee = employeeManagementUseCases.submitForContract(id);
    return EmployeeManagerWebMapper.toResponse(employee, employeePhotoUrlPresenter::present);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeGetById
  public EmployeeResponse getEmployeeDetailById(@PathVariable Long id) {
    Employee employee = employeeManagementUseCases.getById(id);
    return EmployeeManagerWebMapper.toResponse(employee, employeePhotoUrlPresenter::present);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeUpdate
  public EmployeeResponse updateEmployeeMultipart(
      @PathVariable Long id,
      @RequestPart("employee") MultipartFile employeePayload,
      @RequestPart(value = "photo", required = false) MultipartFile photo)
      throws MethodArgumentNotValidException {
    UpdateEmployeeRequest parsed = updateEmployeeMultipartPayloadReader.readAndValidate(employeePayload);
    return updateEmployeeInternal(id, parsed, photo);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeUpdateJsonHidden
  public EmployeeResponse updateEmployeeJsonOnly(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
    return updateEmployeeInternal(id, request, null);
  }

  private EmployeeResponse updateEmployeeInternal(
      Long id, UpdateEmployeeRequest request, MultipartFile photo) {
    Employee updated =
        employeeManagementUseCases.update(id, EmployeeManagerWebMapper.toUpdateParams(id, request, photo));
    return EmployeeManagerWebMapper.toResponse(updated, employeePhotoUrlPresenter::present);
  }

  @PutMapping("/{id}/terminate")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeTerminate
  public EmployeeResponse terminateEmployee(@PathVariable Long id) {
    Employee employee = employeeManagementUseCases.terminate(id);
    return EmployeeManagerWebMapper.toResponse(employee, employeePhotoUrlPresenter::present);
  }

  @PutMapping("/{id}/rehire")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeRehire
  public EmployeeResponse rehireEmployee(@PathVariable Long id) {
    Employee employee = employeeManagementUseCases.rehire(id);
    return EmployeeManagerWebMapper.toResponse(employee, employeePhotoUrlPresenter::present);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeDelete
  public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
    employeeManagementUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }

  private EmployeeResponse registerEmployeeInternal(
      RegisterEmployeeRequest employeeRequest, MultipartFile photo) {
    RegisterEmployeeParams params = EmployeeManagerWebMapper.toRegisterParams(employeeRequest, photo);
    Employee registered = employeeManagementUseCases.register(params);
    return EmployeeManagerWebMapper.toResponse(registered, employeePhotoUrlPresenter::present);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
