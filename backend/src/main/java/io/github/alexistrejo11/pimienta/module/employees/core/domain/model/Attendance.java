package io.github.alexistrejo11.pimienta.module.employees.core.domain.model;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.AttendanceStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Attendance extends BaseDomain<Long> {
  private Long employeeId;
  private Long headquarterId;
  private String checkInEvidencePhotoUrl;
  private String checkOutEvidencePhotoUrl;
  private LocalDate workDate;
  private LocalDateTime checkInTime;
  private LocalDateTime checkOutTime;
  private AttendanceStatus status;

  private Attendance() {
    super();
    this.employeeId = 0L;
    this.headquarterId = 0L;
    this.workDate = LocalDate.now(ClientTimeZones.current());
    this.checkInTime = null;
    this.checkOutTime = null;
    this.status = AttendanceStatus.UNDEFINED;
  }

  /**
   * In-memory row before the first persistence write (id and auditing come from
   * JPA).
   */
  public static Attendance prepareNew() {
    return new Attendance();
  }

  /**
   * Registers a check-in for the given calendar work date (typically “today” in
   * the site
   * timezone).
   */
  public Attendance beginWorkday(
      Long employeeId,
      Long headquarterId,
      LocalDate workDate,
      LocalDateTime checkInAt,
      String checkInEvidencePhotoUrl) {
    this.employeeId = employeeId;
    this.headquarterId = headquarterId;
    this.workDate = workDate;
    this.checkInTime = checkInAt;
    this.checkInEvidencePhotoUrl = checkInEvidencePhotoUrl;
    this.status = AttendanceStatus.CHECKED_IN;
    return this;
  }

  public void completeCheckout(LocalDateTime checkOutAt, String checkOutEvidencePhotoUrl) {
    this.checkOutTime = checkOutAt;
    this.checkOutEvidencePhotoUrl = checkOutEvidencePhotoUrl;
    this.status = AttendanceStatus.CHECKED_OUT;
  }

  public void forceAutoClose(
      LocalDateTime checkOutAt, AttendanceStatus terminalStatus, String checkOutEvidencePhotoUrl) {
    this.checkOutTime = checkOutAt;
    this.checkOutEvidencePhotoUrl = checkOutEvidencePhotoUrl;
    this.status = terminalStatus;
  }

  public long getMinutesWorked() {
    if (checkInTime == null || checkOutTime == null) {
      return 0;
    }
    return Duration.between(checkInTime, checkOutTime).toMinutes();
  }

  public Long getEmployeeId() {
    return employeeId != null ? employeeId : 0L;
  }

  public Long getHeadquarterId() {
    return headquarterId != null ? headquarterId : 0L;
  }

  public String getCheckInEvidencePhotoUrl() {
    return checkInEvidencePhotoUrl != null ? checkInEvidencePhotoUrl : "";
  }

  public String getCheckOutEvidencePhotoUrl() {
    return checkOutEvidencePhotoUrl != null ? checkOutEvidencePhotoUrl : "";
  }

  public LocalDate getWorkDate() {
    return workDate != null ? workDate : LocalDate.now(ClientTimeZones.current());
  }

  public LocalDateTime getCheckInTime() {
    return checkInTime;
  }

  public LocalDateTime getCheckOutTime() {
    return checkOutTime;
  }

  public AttendanceStatus getStatus() {
    return status != null ? status : AttendanceStatus.UNDEFINED;
  }

  public static Attendance restore(
      Long id,
      Long employeeId,
      Long headquarterId,
      String checkInEvidencePhotoUrl,
      String checkOutEvidencePhotoUrl,
      LocalDate workDate,
      LocalDateTime checkInTime,
      LocalDateTime checkOutTime,
      AttendanceStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      LocalDateTime deletedAt,
      Long version) {
    var a = new Attendance();
    a.id = id;
    a.employeeId = employeeId;
    a.headquarterId = headquarterId;
    a.checkInEvidencePhotoUrl = checkInEvidencePhotoUrl;
    a.checkOutEvidencePhotoUrl = checkOutEvidencePhotoUrl;
    a.workDate = workDate;
    a.checkInTime = checkInTime;
    a.checkOutTime = checkOutTime;
    a.status = status;
    a.createdAt = createdAt;
    a.updatedAt = updatedAt;
    a.deletedAt = deletedAt;
    a.version = version;
    return a;
  }
}
