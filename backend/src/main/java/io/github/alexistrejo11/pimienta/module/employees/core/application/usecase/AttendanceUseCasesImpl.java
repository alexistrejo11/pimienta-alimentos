package io.github.alexistrejo11.pimienta.module.employees.core.application.usecase;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.employees.core.application.command.EndWorkdayCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.command.StartWorkdayCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.AttendanceSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.AttendanceStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.AttendanceAlreadyOpenException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.AttendanceNotFoundException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.AttendanceNotOpenForCheckoutException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.exception.EmployeeNotFoundException;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Attendance;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.AttendanceAutoCloseUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.AttendanceQueryUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.AttendanceTrackingUseCases;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.AttendanceRepository;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeRepository;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeStorageService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AttendanceUseCasesImpl
    implements AttendanceTrackingUseCases, AttendanceQueryUseCases, AttendanceAutoCloseUseCases {

  private static final Logger log = LoggerFactory.getLogger(AttendanceUseCasesImpl.class);

  static final int MAX_OPEN_SHIFT_HOURS = 12;
  static final int DEFAULT_CONTRACT_DAY_HOURS = 8;

  private final AttendanceRepository attendanceRepository;
  private final EmployeeRepository employeeRepository;
  private final HeadquarterRepository headquarterRepository;
  private final EmployeeStorageService employeeStorageService;
  private final TransactionTemplate transactionTemplate;

  public AttendanceUseCasesImpl(
      AttendanceRepository attendanceRepository,
      EmployeeRepository employeeRepository,
      HeadquarterRepository headquarterRepository,
      EmployeeStorageService employeeStorageService,
      PlatformTransactionManager transactionManager) {
    this.attendanceRepository = attendanceRepository;
    this.employeeRepository = employeeRepository;
    this.headquarterRepository = headquarterRepository;
    this.employeeStorageService = employeeStorageService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public Attendance startWorkday(StartWorkdayCommand command) {
    String evidenceKey =
        uploadEvidence(command.checkInEvidencePhoto(), command.employeeId(), "check-in");
    try {
      Attendance saved =
          transactionTemplate.execute(
              status -> {
                employeeRepository
                    .findById(command.employeeId())
                    .orElseThrow(() -> new EmployeeNotFoundException(command.employeeId()));
                headquarterRepository
                    .findById(command.headquarterId())
                    .orElseThrow(() -> new HeadquarterNotFoundException(command.headquarterId()));
                if (attendanceRepository
                    .findOpenForEmployeeOnWorkDate(command.employeeId(), command.workDate())
                    .isPresent()) {
                  throw new AttendanceAlreadyOpenException(command.employeeId(), command.workDate());
                }
                Attendance row = Attendance.prepareNew();
                row.beginWorkday(
                    command.employeeId(),
                    command.headquarterId(),
                    command.workDate(),
                    utcNow(),
                    evidenceKey == null ? "" : evidenceKey);
                return attendanceRepository.save(row);
              });
      log.debug(
          "attendance check-in saved id={} employeeId={} headquarterId={} workDate={}",
          saved.getId(),
          saved.getEmployeeId(),
          saved.getHeadquarterId(),
          saved.getWorkDate());
      return saved;
    } catch (RuntimeException ex) {
      employeeStorageService.deleteQuietly(evidenceKey);
      throw ex;
    }
  }

  @Override
  public Attendance endWorkday(EndWorkdayCommand command) {
    String evidenceKey =
        uploadEvidence(command.checkOutEvidencePhoto(), command.employeeId(), "check-out");
    try {
      return transactionTemplate.execute(
          status -> {
            employeeRepository
                .findById(command.employeeId())
                .orElseThrow(() -> new EmployeeNotFoundException(command.employeeId()));
            Attendance open =
                attendanceRepository
                    .findOpenForEmployeeOnWorkDate(command.employeeId(), command.workDate())
                    .orElseThrow(
                        () ->
                            new AttendanceNotOpenForCheckoutException(
                                command.employeeId(), command.workDate()));
            open.completeCheckout(utcNow(), evidenceKey == null ? "" : evidenceKey);
            return attendanceRepository.save(open);
          });
    } catch (RuntimeException ex) {
      employeeStorageService.deleteQuietly(evidenceKey);
      throw ex;
    }
  }

  private String uploadEvidence(ImageUploadPayload photo, long employeeId, String kind) {
    if (photo == null || photo.isEmpty()) {
      return null;
    }
    return employeeStorageService.uploadAttendanceEvidencePhoto(photo, employeeId, kind);
  }

  @Override
  @Transactional(readOnly = true)
  public Attendance getById(Long id) {
    return attendanceRepository.findById(id).orElseThrow(() -> new AttendanceNotFoundException(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Attendance> listForToday(Long headquarterId, Pageable pageable) {
    return attendanceRepository.findPageForWorkDate(
        LocalDate.now(ClientTimeZones.current()), headquarterId, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Attendance> listByEmployee(
      long employeeId, LocalDate workDateFrom, LocalDate workDateTo, Pageable pageable) {
    employeeRepository.findById(employeeId).orElseThrow(() -> new EmployeeNotFoundException(employeeId));
    if (workDateFrom != null && workDateTo != null && workDateTo.isBefore(workDateFrom)) {
      throw new IllegalArgumentException("workDateTo must not be before workDateFrom");
    }
    AttendanceSearchCriteria criteria =
        new AttendanceSearchCriteria(employeeId, null, null, workDateFrom, workDateTo, null, null);
    return attendanceRepository.search(criteria, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Attendance> search(AttendanceSearchCriteria criteria, Pageable pageable) {
    AttendanceSearchCriteria effective = criteria != null ? criteria : AttendanceSearchCriteria.empty();
    return attendanceRepository.search(effective, pageable);
  }

  @Override
  @Transactional
  public void runStaleOpenAttendanceSweep() {
    LocalDateTime now = utcNow();
    LocalDate today = LocalDate.now(ClientTimeZones.current());
    List<Attendance> openRows = attendanceRepository.findAllOpenCheckedIn();
    int closedExcess = 0;
    int closedAssumed = 0;
    for (Attendance a : openRows) {
      LocalDateTime checkIn = a.getCheckInTime();
      if (checkIn == null) {
        continue;
      }
      long hoursOpen = Duration.between(checkIn, now).toHours();
      if (hoursOpen >= MAX_OPEN_SHIFT_HOURS) {
        a.forceAutoClose(
            checkIn.plusHours(MAX_OPEN_SHIFT_HOURS),
            AttendanceStatus.AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS,
            null);
        attendanceRepository.save(a);
        closedExcess++;
      } else if (a.getWorkDate().isBefore(today)
          && now.isAfter(checkIn.plusHours(DEFAULT_CONTRACT_DAY_HOURS))) {
        a.forceAutoClose(
            checkIn.plusHours(DEFAULT_CONTRACT_DAY_HOURS),
            AttendanceStatus.AUTO_CLOSED_ASSUMED_CONTRACT_DAY,
            null);
        attendanceRepository.save(a);
        closedAssumed++;
      }
    }
    if (closedExcess > 0 || closedAssumed > 0) {
      log.info(
          "attendance stale sweep closed rows excessHours={} assumedEightHourDay={}",
          closedExcess,
          closedAssumed);
    }
  }

  /** Stored attendance instants are UTC wall times from {@code UtcLocalDateTimeConverter}. */
  private static LocalDateTime utcNow() {
    return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
  }
}
