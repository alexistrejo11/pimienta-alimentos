package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.controller;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeWorkScheduleGet;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeWorkSchedulePut;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc.DocEmployeeWorkSchedules;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.ReplaceEmployeeWorkScheduleRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeWorkScheduleResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper.EmployeeWorkScheduleWebMapper;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.EmployeeWorkSchedule;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeWorkScheduleUseCases;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/employees")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocEmployeeWorkSchedules
public class EmployeeWorkScheduleController {

  private final EmployeeWorkScheduleUseCases workScheduleUseCases;

  public EmployeeWorkScheduleController(EmployeeWorkScheduleUseCases workScheduleUseCases) {
    this.workScheduleUseCases = workScheduleUseCases;
  }

  @GetMapping("/{employeeId}/work-schedule")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocEmployeeWorkScheduleGet
  public EmployeeWorkScheduleResponse getEmployeeWorkSchedule(@PathVariable Long employeeId) {
    EmployeeWorkSchedule schedule = workScheduleUseCases.getByEmployeeId(employeeId);
    return EmployeeWorkScheduleWebMapper.toResponse(schedule);
  }

  @PutMapping("/{employeeId}/work-schedule")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocEmployeeWorkSchedulePut
  public EmployeeWorkScheduleResponse replaceEmployeeWorkSchedule(
      @PathVariable Long employeeId,
      @Valid @RequestBody ReplaceEmployeeWorkScheduleRequest request) {
    EmployeeWorkSchedule schedule = EmployeeWorkScheduleWebMapper.toDomain(request);

    EmployeeWorkSchedule scheduleSaved = workScheduleUseCases.replace(employeeId, schedule);
    return EmployeeWorkScheduleWebMapper.toResponse(scheduleSaved);
  }
}
