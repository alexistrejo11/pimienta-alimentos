package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.mapper;

import org.springframework.stereotype.Component;

import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.UserManagerDashboard;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.UpdateProfileCommand;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UpdateProfileRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserDashboardResponse;

@Component
public final class ProfileWebMapper {

  private ProfileWebMapper() {
  }

  public static UpdateProfileCommand toCommand(UpdateProfileRequest request) {
    return new UpdateProfileCommand(
        request.firstName(),
        request.lastName(),
        request.gender(),
        request.phone());
  }

  public static UserDashboardResponse toDashboardResponse(UserManagerDashboard dashboard) {
    return new UserDashboardResponse(
        dashboard.totalActiveEmployees(),
        dashboard.totalActiveProjects(),
        dashboard.totalActiveHeadquarters(),
        dashboard.totalActivePersonalTasks(),
        dashboard.totalPendingPersonalTasks(),
        dashboard.totalActiveEmployeesTasks(),
        dashboard.totalEmployeePending());
  }
}
