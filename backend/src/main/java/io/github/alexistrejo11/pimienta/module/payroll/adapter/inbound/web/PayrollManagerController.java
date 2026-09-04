package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayroll;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollCreatePeriod;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollCreateRecord;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollExport;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollImport;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollListDebts;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollListPayments;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollListPeriods;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollListRecords;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollRegisterAdjustment;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollRegisterPayment;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc.DocPayrollSummary;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.PayrollBulkScopeRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.PayrollRecordSearchRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollAdjustmentRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollPeriodRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollPaymentRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request.RegisterPayrollRecordRequest;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollDebtResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollPaymentResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollPeriodResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollRecordResponse;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response.PayrollSummaryResponse;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollPeriodCommand;
import io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.mapper.PayrollManagerWebMapper;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.PayrollBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollAdjustmentCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollPaymentCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.command.RegisterPayrollRecordCommand;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.query.PayrollBulkScopeQuery;
import io.github.alexistrejo11.pimienta.module.payroll.core.application.query.PayrollSummaryQuery;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollDebt;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPayment;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollPeriod;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollRecord;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.PayrollSummary;
import io.github.alexistrejo11.pimienta.module.payroll.core.port.input.PayrollManagementUseCases;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/payroll")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPayroll
public class PayrollManagerController {

  private final PayrollManagementUseCases payrollManagementUseCases;
  private final PayrollBulkSyncUseCases payrollBulkSyncUseCases;

  public PayrollManagerController(
      PayrollManagementUseCases payrollManagementUseCases,
      PayrollBulkSyncUseCases payrollBulkSyncUseCases) {
    this.payrollManagementUseCases = payrollManagementUseCases;
    this.payrollBulkSyncUseCases = payrollBulkSyncUseCases;
  }

  @GetMapping("/payments")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollListPayments
  public PagedResponse<PayrollPaymentResponse> listPayrollPayments(
      @RequestParam(required = false) Long employeeId, @ModelAttribute PageableRequest pageable) {
    Page<PayrollPayment> page = payrollManagementUseCases.listPaymentsByEmployee(employeeId, pageable.toPageable());
    return PagedResponse.map(page, PayrollManagerWebMapper::toPaymentResponse);
  }

  @GetMapping("/periods")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollListPeriods
  public PagedResponse<PayrollPeriodResponse> listPayrollPeriods(
      @ModelAttribute PageableRequest pageable) {
    Page<PayrollPeriod> page = payrollManagementUseCases.listPeriods(pageable.toPageable());
    return PagedResponse.map(page, PayrollManagerWebMapper::toPeriodResponse);
  }

  @PostMapping("/periods")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPayrollCreatePeriod
  public PayrollPeriodResponse registerPeriod(@Valid @RequestBody RegisterPayrollPeriodRequest request) {
    RegisterPayrollPeriodCommand command = PayrollManagerWebMapper.toCommand(request);
    PayrollPeriod created = payrollManagementUseCases.registerPeriod(command);
    return PayrollManagerWebMapper.toPeriodResponse(created);
  }

  @GetMapping("/records")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollListRecords
  public PagedResponse<PayrollRecordResponse> listPayrollRecords(@ModelAttribute PayrollRecordSearchRequest request) {
    Page<PayrollRecord> page =
        payrollManagementUseCases.listRecords(
            request.getEmployeeId(), request.getPeriodId(), request.toPageable());
    return PagedResponse.map(page, PayrollManagerWebMapper::toRecordResponse);
  }

  @GetMapping("/export")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollExport
  public ResponseEntity<byte[]> exportPayrollRecords(@ModelAttribute PayrollBulkScopeRequest request)
      throws IOException {
    PayrollBulkScopeQuery scope =
        new PayrollBulkScopeQuery(
            request.getEmployeeId(),
            request.getPeriodId(),
            request.getFrom(),
            request.getTo());
    byte[] bytes = payrollBulkSyncUseCases.exportPayrollRecords(scope, request.toPageable());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payroll_reporte.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPayrollImport
  public SpreadsheetBulkImportResult importPayrollRecords(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun,
      @ModelAttribute PayrollBulkScopeRequest request)
      throws IOException {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    PayrollBulkScopeQuery scope =
        new PayrollBulkScopeQuery(
            request.getEmployeeId(),
            request.getPeriodId(),
            request.getFrom(),
            request.getTo());
    return payrollBulkSyncUseCases.importPayrollRecords(
        file.getInputStream(), file.getOriginalFilename(), scope, dryRun);
  }

  @PostMapping("/records")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPayrollCreateRecord
  public PayrollRecordResponse registerPayrollRecord(@Valid @RequestBody RegisterPayrollRecordRequest request) {
    RegisterPayrollRecordCommand command = PayrollManagerWebMapper.toCommand(request);
    PayrollRecord created = payrollManagementUseCases.registerRecord(command);
    return PayrollManagerWebMapper.toRecordResponse(created);
  }

  @GetMapping("/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollSummary
  public PayrollSummaryResponse getPayrollSummary(
      @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
    PayrollSummary summary = payrollManagementUseCases.summary(new PayrollSummaryQuery(from, to));
    return PayrollManagerWebMapper.toSummaryResponse(summary);
  }

  @GetMapping("/debts")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPayrollListDebts
  public PagedResponse<PayrollDebtResponse> listDebts(
      @ModelAttribute PageableRequest pageable) {
    Page<PayrollDebt> page = payrollManagementUseCases.listDebts(pageable.toPageable());
    return PagedResponse.map(page, PayrollManagerWebMapper::toDebtResponse);
  }

  @PostMapping("/payments")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPayrollRegisterPayment
  public PayrollPaymentResponse registerPayrollPayment(
      @Valid @RequestBody RegisterPayrollPaymentRequest request) {
    RegisterPayrollPaymentCommand command = PayrollManagerWebMapper.toCommand(request);
    PayrollPayment payment = payrollManagementUseCases.registerPayment(command);
    return PayrollManagerWebMapper.toPaymentResponse(payment);
  }

  @PostMapping("/records/{recordId}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPayrollRegisterAdjustment
  public PayrollPaymentResponse registerAdjustment(
      @PathVariable Long recordId, @Valid @RequestBody RegisterPayrollAdjustmentRequest request) {
    RegisterPayrollAdjustmentCommand command = PayrollManagerWebMapper.toCommand(recordId, request);
    PayrollPayment payment = payrollManagementUseCases.registerAdjustment(command);
    return PayrollManagerWebMapper.toPaymentResponse(payment);
  }
}
