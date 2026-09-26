package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LoginCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LogoutCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RefreshSessionCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RegisterCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.IssuedTokens;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.LoginRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.LogoutRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RefreshRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RegisterRequest;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.TokenResponse;

public final class AuthWebMapper {

  private AuthWebMapper() {
  }

  public static RegisterCommand toRegisterCommand(RegisterRequest request) {
    return new RegisterCommand(
        request.email(),
        request.password(),
        request.firstName(),
        request.lastName(),
        request.gender(),
        request.phone());
  }

  public static LoginCommand toLoginCommand(LoginRequest request) {
    return new LoginCommand(request.email(), request.password());
  }

  public static RefreshSessionCommand toRefreshCommand(RefreshRequest request) {
    return new RefreshSessionCommand(request.refreshToken());
  }

  public static LogoutCommand toLogoutCommand(LogoutRequest request) {
    return new LogoutCommand(request != null ? request.refreshToken() : null);
  }

  public static TokenResponse toResponse(IssuedTokens tokens) {
    return new TokenResponse(
        tokens.accessToken(), tokens.refreshToken(), "Bearer", tokens.accessExpiresInSeconds());
  }
}
