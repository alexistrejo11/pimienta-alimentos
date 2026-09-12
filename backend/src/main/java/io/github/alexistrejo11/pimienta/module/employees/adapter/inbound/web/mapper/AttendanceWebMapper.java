package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.AttendanceResponse;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Attendance;
import java.util.function.UnaryOperator;

public class AttendanceWebMapper {

  private AttendanceWebMapper() {
  }

  public static AttendanceResponse toResponse(
      Attendance a, UnaryOperator<String> evidenceUrlPresenter, String employeeFullName) {
    return new AttendanceResponse(
        a.getId(),
        a.getEmployeeId(),
        employeeFullName != null ? employeeFullName : "",
        a.getHeadquarterId(),
        a.getWorkDate(),
        a.getCheckInTime(),
        a.getCheckOutTime(),
        a.getStatus(),
        evidenceUrlPresenter.apply(a.getCheckInEvidencePhotoUrl()),
        evidenceUrlPresenter.apply(a.getCheckOutEvidencePhotoUrl()),
        a.getMinutesWorked());
  }
}
