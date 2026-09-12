package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AddRolesCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.AssignHeadquartersCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.BanUserCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserStatistics;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.input.UserManagementUseCases;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagement;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementAddRoles;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementApproveUser;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementBanUser;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementGetUserByEmail;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementGetUserById;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementListUsers;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementStatistics;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserManagementUnbanUser;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.AddRolesRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.AssignHeadquartersRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.BanUserRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserResponse;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserStatisticsResponse;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.mapper.UserManagerWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/users/management")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocUserManagement
public class UserManagerController {

  private final UserManagementUseCases userManagementUseCases;

  public UserManagerController(UserManagementUseCases userManagementUseCases) {
    this.userManagementUseCases = userManagementUseCases;
  }

  // TODO: Add support for filtering to customize the statistics response.
  @GetMapping("/statistics")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserManagementStatistics
  public UserStatisticsResponse getStatistics() {
    UserStatistics statistics = userManagementUseCases.statistics();
    return UserManagerWebMapper.toStatisticsResponse(statistics);
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserManagementListUsers
  public PagedResponse<UserResponse> listUsers(@ModelAttribute PageableRequest pageRequest) {
    var pageable = pageRequest.toPageable();
    Page<User> users = userManagementUseCases.getBy(pageable);

    Page<UserResponse> usersResponse = users.map(UserManagerWebMapper::toResponse);
    return PagedResponse.of(usersResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserManagementGetUserById
  public UserResponse getUserById(@PathVariable Long id) {
    User user = userManagementUseCases.getById(id);
    return UserManagerWebMapper.toResponse(user);
  }

  @GetMapping("/by-email")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserManagementGetUserByEmail
  public UserResponse getUserByEmail(@RequestParam @Email String email) {
    var emailNormalized = email.trim().toLowerCase();
    User user = userManagementUseCases.getByEmail(emailNormalized);

    return UserManagerWebMapper.toResponse(user);
  }

  @PostMapping("/{id}/ban")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocUserManagementBanUser
  public void banUser(
      @PathVariable Long id, @Valid @RequestBody(required = false) BanUserRequest request) {
    BanUserCommand command = UserManagerWebMapper.toBanCommand(request);
    userManagementUseCases.ban(id, command);
  }

  @PostMapping("/{id}/unban")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocUserManagementUnbanUser
  public void unbanUser(@PathVariable Long id) {
    userManagementUseCases.unban(id);
  }

  @PostMapping("/{id}/roles")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocUserManagementAddRoles
  public UserResponse addRoles(@PathVariable Long id, @Valid @RequestBody AddRolesRequest request) {
    AddRolesCommand command = UserManagerWebMapper.toAddRolesCommand(request);
    User updated = userManagementUseCases.addRoles(id, command);
    return UserManagerWebMapper.toResponse(updated);
  }

  @PostMapping("/{id}/headquarters")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public UserResponse assignHeadquarters(
      @PathVariable Long id, @Valid @RequestBody AssignHeadquartersRequest request) {
    AssignHeadquartersCommand command = UserManagerWebMapper.toAssignHeadquartersCommand(request);
    User updated = userManagementUseCases.assignHeadquarters(id, command);
    return UserManagerWebMapper.toResponse(updated);
  }

  @PostMapping("/{id}/approve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocUserManagementApproveUser
  public void approveUser(@PathVariable Long id) {
    userManagementUseCases.approve(id);
  }
}
