package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.account.auth.core.application.RegisterResult;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LoginCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LogoutCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RefreshSessionCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RegisterCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.IssuedTokens;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.AuthUseCases;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc.DocAuth;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc.DocAuthLogin;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc.DocAuthLogout;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc.DocAuthRefresh;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc.DocAuthRegister;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.LoginRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.LogoutRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RefreshRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RegisterRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RegisterResponse;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.TokenResponse;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.mapper.AuthWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/auth")
@DocAuth
public class AuthController {

  private final AuthUseCases authUseCases;

  public AuthController(AuthUseCases authUseCases) {
    this.authUseCases = authUseCases;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.STRICT)
  @DocAuthRegister
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    RegisterCommand command = AuthWebMapper.toRegisterCommand(request);
    RegisterResult result = authUseCases.register(command);
    String message =
        result.requireAdminActivation()
            ? "Registro exitoso. Tu cuenta está pendiente de aprobación por un administrador."
            : "Registro exitoso.";
    return new RegisterResponse(message, "PENDING_APPROVAL");
  }

  @PostMapping("/login")
  @RateLimit(profile = RateLimitProfile.STRICT)
  @DocAuthLogin
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    LoginCommand command = AuthWebMapper.toLoginCommand(request);
    IssuedTokens issued = authUseCases.login(command);
    return AuthWebMapper.toResponse(issued);
  }

  @PostMapping("/refresh")
  @RateLimit(profile = RateLimitProfile.AUTH_SESSION)
  @DocAuthRefresh
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    RefreshSessionCommand command = AuthWebMapper.toRefreshCommand(request);
    IssuedTokens issued = authUseCases.refresh(command);
    return AuthWebMapper.toResponse(issued);
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.AUTH_SESSION)
  @DocAuthLogout
  public void logout(@RequestBody(required = false) LogoutRequest request) {
    LogoutCommand command = AuthWebMapper.toLogoutCommand(request);
    authUseCases.logout(command);
  }
}
