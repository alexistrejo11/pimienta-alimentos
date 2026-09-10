package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AddRolesCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AssignHeadquartersCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.BanUserCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserStatistics;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.AddRolesRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.AssignHeadquartersRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.BanUserRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserResponse;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserStatisticsResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class UserManagerWebMapper {

  public UserManagerWebMapper() {
  }

  public static UserStatisticsResponse toStatisticsResponse(UserStatistics statistics) {
    return new UserStatisticsResponse(
        statistics.totalUsers(), statistics.activeUsers(), statistics.bannedUsers());
  }

  public static UserResponse toResponse(User user) {
    List<String> roleNames = user.getRoles().stream().map(Enum::name).toList();
    var permSet = new HashSet<String>();
    for (Role r : user.getRoles()) {
      r.getPermissions().forEach(p -> permSet.add(p.name()));
    }
    var permissions = new ArrayList<>(permSet);
    permissions.sort(String::compareTo);
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getGender(),
        user.getPhone(),
        user.getDateOfBirth(),
        user.getAccountStatus(),
        user.getBannedReason(),
        user.getBannedAt(),
        roleNames,
        permissions,
        user.getAssignedHeadquarterIds(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  public static BanUserCommand toBanCommand(BanUserRequest request) {
    if (request == null) {
      return new BanUserCommand(null);
    }
    return new BanUserCommand(request.reason());
  }

  public static AddRolesCommand toAddRolesCommand(AddRolesRequest request) {
    List<Role> roles = new ArrayList<>();
    for (String name : request.roles()) {
      roles.add(Role.valueOf(name.trim().toUpperCase()));
    }
    return new AddRolesCommand(roles);
  }

  public static AssignHeadquartersCommand toAssignHeadquartersCommand(AssignHeadquartersRequest request) {
    return new AssignHeadquartersCommand(request.headquarterIds());
  }
}
