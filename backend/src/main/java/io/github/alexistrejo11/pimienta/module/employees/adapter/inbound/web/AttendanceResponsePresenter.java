package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.AttendanceResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.AttendanceWebMapper;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Attendance;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class AttendanceResponsePresenter {

  private final EmployeePhotoUrlPresenter employeePhotoUrlPresenter;
  private final EmployeeRepository employeeRepository;

  public AttendanceResponsePresenter(
      EmployeePhotoUrlPresenter employeePhotoUrlPresenter, EmployeeRepository employeeRepository) {
    this.employeePhotoUrlPresenter = employeePhotoUrlPresenter;
    this.employeeRepository = employeeRepository;
  }

  public AttendanceResponse toResponse(Attendance attendance) {
    String employeeFullName = resolveEmployeeName(attendance.getEmployeeId());
    return AttendanceWebMapper.toResponse(
        attendance, employeePhotoUrlPresenter::present, employeeFullName);
  }

  public PagedResponse<AttendanceResponse> toResponsePage(Page<Attendance> page) {
    Map<Long, String> names = resolveEmployeeNames(
        page.getContent().stream().map(Attendance::getEmployeeId).collect(Collectors.toSet()));
    return PagedResponse.map(
        page,
        a ->
            AttendanceWebMapper.toResponse(
                a,
                employeePhotoUrlPresenter::present,
                names.getOrDefault(a.getEmployeeId(), "#" + a.getEmployeeId())));
  }

  private String resolveEmployeeName(Long employeeId) {
    if (employeeId == null) {
      return "";
    }
    return employeeRepository
        .findById(employeeId)
        .map(this::formatName)
        .orElse("#" + employeeId);
  }

  private Map<Long, String> resolveEmployeeNames(Set<Long> employeeIds) {
    Map<Long, String> names = new HashMap<>();
    List<Long> ids =
        employeeIds.stream().filter(Objects::nonNull).distinct().toList();
    for (Long id : ids) {
      names.put(id, resolveEmployeeName(id));
    }
    return names;
  }

  private String formatName(Employee employee) {
    return employee.getPersonal().firstName() + " " + employee.getPersonal().lastName();
  }
}
