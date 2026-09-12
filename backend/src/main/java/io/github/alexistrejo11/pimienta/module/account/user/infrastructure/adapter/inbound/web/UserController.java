package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.UserManagerDashboard;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.ProfileUseCases;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.command.UpdateProfileCommand;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserProfile;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserProfileDashboard;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserProfileGetMe;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc.DocUserProfilePatchMe;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UpdateProfileRequest;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserDashboardResponse;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserResponse;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.mapper.ProfileWebMapper;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.mapper.UserManagerWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/users")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocUserProfile
public class UserController {

  private final ProfileUseCases profileUseCases;

  public UserController(ProfileUseCases profileUseCases) {
    this.profileUseCases = profileUseCases;
  }

  @GetMapping("/me")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserProfileGetMe
  public UserResponse getProfile(@AuthenticationPrincipal JwtAuthenticationContext principal) {
    User user = profileUseCases.getProfile(principal.userId());
    return UserManagerWebMapper.toResponse(user);
  }

  @PatchMapping("/me")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocUserProfilePatchMe
  public UserResponse updateProfile(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody UpdateProfileRequest request) {
    UpdateProfileCommand command = ProfileWebMapper.toCommand(request);
    User user = profileUseCases.updateProfile(principal.userId(), command);

    return UserManagerWebMapper.toResponse(user);
  }

  @GetMapping("/me/dashboard")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocUserProfileDashboard
  public UserDashboardResponse getDashboard(
      @AuthenticationPrincipal JwtAuthenticationContext principal) {
    UserManagerDashboard dashboard = profileUseCases.getDashboard(principal.userId());
    return ProfileWebMapper.toDashboardResponse(dashboard);
  }
}
