package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.AttendanceJpaEntity;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.AttendanceStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Attendance;

import java.time.LocalDateTime;

public class AttendancePersistenceMapper {

  private AttendancePersistenceMapper() {
  }

  public static AttendanceJpaEntity toJpa(Attendance domain) {
    AttendanceJpaEntity e = new AttendanceJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setEmployeeId(domain.getEmployeeId());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setWorkDate(domain.getWorkDate());
    e.setCheckInTime(domain.getCheckInTime());
    e.setCheckOutTime(domain.getCheckOutTime());
    e.setStatus(domain.getStatus());
    e.setCheckInEvidencePhotoUrl(blankToNull(domain.getCheckInEvidencePhotoUrl()));
    e.setCheckOutEvidencePhotoUrl(blankToNull(domain.getCheckOutEvidencePhotoUrl()));
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    if (domain.getId() != null && domain.getId() > 0) {
      e.setUpdatedAt(LocalDateTime.now());
    }
    e.fillCreatedAndUpdatedIfNull();
    e.fillUpdatedIfNull();
    return e;
  }

  public static Attendance toDomain(AttendanceJpaEntity e) {
    return Attendance.restore(
        e.getId(),
        e.getEmployeeId(),
        e.getHeadquarterId(),
        nullToEmpty(e.getCheckInEvidencePhotoUrl()),
        nullToEmpty(e.getCheckOutEvidencePhotoUrl()),
        e.getWorkDate(),
        e.getCheckInTime(),
        e.getCheckOutTime(),
        e.getStatus() != null ? e.getStatus() : AttendanceStatus.UNDEFINED,
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getDeletedAt(),
        e.getVersion());
  }

  private static String blankToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String nullToEmpty(String s) {
    return s != null ? s : "";
  }
}
