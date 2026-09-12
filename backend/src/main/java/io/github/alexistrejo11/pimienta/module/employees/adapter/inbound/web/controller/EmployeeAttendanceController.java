package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.controller;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.AttendanceResponsePresenter;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceEndWorkday;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceEndWorkdayJsonHidden;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceGetById;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceListByEmployee;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceListForToday;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceSearch;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceStartWorkday;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocAttendanceStartWorkdayJsonHidden;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeAttendances;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.EndWorkdayRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.StartWorkdayRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.AttendanceResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.AttendanceMultipartPayloadReader;
import io.github.alexistrejo11.pimienta.module.employees.core.application.command.EndWorkdayCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.command.StartWorkdayCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.AttendanceSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.AttendanceStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Attendance;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.AttendanceQueryUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.AttendanceTrackingUseCases;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(BASE + "/employees")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocEmployeeAttendances
public class EmployeeAttendanceController {

  private final AttendanceTrackingUseCases attendanceTrackingUseCases;
  private final AttendanceQueryUseCases attendanceQueryUseCases;
  private final AttendanceMultipartPayloadReader attendanceMultipartPayloadReader;
  private final AttendanceResponsePresenter attendanceResponsePresenter;

  public EmployeeAttendanceController(
      AttendanceTrackingUseCases attendanceTrackingUseCases,
      AttendanceQueryUseCases attendanceQueryUseCases,
      AttendanceMultipartPayloadReader attendanceMultipartPayloadReader,
      AttendanceResponsePresenter attendanceResponsePresenter) {
    this.attendanceTrackingUseCases = attendanceTrackingUseCases;
    this.attendanceQueryUseCases = attendanceQueryUseCases;
    this.attendanceMultipartPayloadReader = attendanceMultipartPayloadReader;
    this.attendanceResponsePresenter = attendanceResponsePresenter;
  }

  @PostMapping(value = "/{employeeId}/attendance/start-workday", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocAttendanceStartWorkday
  public AttendanceResponse startWorkdayMultipart(
      @PathVariable Long employeeId,
      @RequestPart("attendance") MultipartFile attendancePayload,
      @RequestPart(value = "checkInEvidencePhoto", required = false) MultipartFile checkInEvidencePhoto)
      throws MethodArgumentNotValidException {
    StartWorkdayRequest request = attendanceMultipartPayloadReader.readStartWorkday(attendancePayload);
    return startWorkdayInternal(employeeId, request, checkInEvidencePhoto);
  }

  @PostMapping(value = "/{employeeId}/attendance/start-workday", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocAttendanceStartWorkdayJsonHidden
  public AttendanceResponse startWorkdayJson(
      @PathVariable Long employeeId, @Valid @RequestBody StartWorkdayRequest request) {
    return startWorkdayInternal(employeeId, request, null);
  }

  @PostMapping(value = "/{employeeId}/attendance/end-workday", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocAttendanceEndWorkday
  public AttendanceResponse endWorkdayMultipart(
      @PathVariable Long employeeId,
      @RequestPart("attendance") MultipartFile attendancePayload,
      @RequestPart(value = "checkOutEvidencePhoto", required = false) MultipartFile checkOutEvidencePhoto)
      throws MethodArgumentNotValidException {
    EndWorkdayRequest request = attendanceMultipartPayloadReader.readEndWorkday(attendancePayload);
    return endWorkdayInternal(employeeId, request, checkOutEvidencePhoto);
  }

  @PostMapping(value = "/{employeeId}/attendance/end-workday", consumes = MediaType.APPLICATION_JSON_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocAttendanceEndWorkdayJsonHidden
  public AttendanceResponse endWorkdayJson(
      @PathVariable Long employeeId, @Valid @RequestBody EndWorkdayRequest request) {
    return endWorkdayInternal(employeeId, request, null);
  }

  @GetMapping("/attendances/search")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocAttendanceSearch
  public PagedResponse<AttendanceResponse> searchAttendances(
      @RequestParam(required = false) Long employeeId,
      @RequestParam(required = false) Long headquarterId,
      @RequestParam(required = false) LocalDate workDate,
      @RequestParam(required = false) LocalDate workDateFrom,
      @RequestParam(required = false) LocalDate workDateTo,
      @RequestParam(required = false) AttendanceStatus status,
      @RequestParam(required = false) Boolean onlyOpen,
      @ModelAttribute PageableRequest pageable) {
    AttendanceSearchCriteria criteria = new AttendanceSearchCriteria(
        employeeId, headquarterId, workDate, workDateFrom, workDateTo, status, onlyOpen);
    Page<Attendance> page = attendanceQueryUseCases.search(criteria, pageable.toPageable());
    return attendanceResponsePresenter.toResponsePage(page);
  }

  @GetMapping("/attendances/for-today")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocAttendanceListForToday
  public PagedResponse<AttendanceResponse> listAttendancesForToday(
      @RequestParam(required = false) Long headquarterId, @ModelAttribute PageableRequest pageable) {
    Page<Attendance> page = attendanceQueryUseCases.listForToday(headquarterId, pageable.toPageable());
    return attendanceResponsePresenter.toResponsePage(page);
  }

  @GetMapping("/{employeeId}/attendances")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocAttendanceListByEmployee
  public PagedResponse<AttendanceResponse> listAttendancesByEmployee(
      @PathVariable Long employeeId,
      @RequestParam(required = false) LocalDate workDateFrom,
      @RequestParam(required = false) LocalDate workDateTo,
      @ModelAttribute PageableRequest pageable) {
    Page<Attendance> page =
        attendanceQueryUseCases.listByEmployee(
            employeeId, workDateFrom, workDateTo, pageable.toPageable());
    return attendanceResponsePresenter.toResponsePage(page);
  }

  @GetMapping("/attendances/{attendanceId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocAttendanceGetById
  public AttendanceResponse getAttendanceById(@PathVariable Long attendanceId) {
    Attendance row = attendanceQueryUseCases.getById(attendanceId);
    return attendanceResponsePresenter.toResponse(row);
  }

  private AttendanceResponse startWorkdayInternal(
      Long employeeId, StartWorkdayRequest request, MultipartFile checkInEvidencePhoto) {
    LocalDate workDate = request.workDate() != null ? request.workDate() : LocalDate.now();
    StartWorkdayCommand command =
        new StartWorkdayCommand(employeeId, request.headquarterId(), workDate, checkInEvidencePhoto);
    Attendance saved = attendanceTrackingUseCases.startWorkday(command);
    return attendanceResponsePresenter.toResponse(saved);
  }

  private AttendanceResponse endWorkdayInternal(
      Long employeeId, EndWorkdayRequest request, MultipartFile checkOutEvidencePhoto) {
    LocalDate workDate = request.workDate() != null ? request.workDate() : LocalDate.now();
    EndWorkdayCommand command = new EndWorkdayCommand(employeeId, workDate, checkOutEvidencePhoto);
    Attendance saved = attendanceTrackingUseCases.endWorkday(command);
    return attendanceResponsePresenter.toResponse(saved);
  }
}
