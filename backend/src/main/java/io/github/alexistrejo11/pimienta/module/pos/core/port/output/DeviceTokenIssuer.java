package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import java.util.Map;
import java.util.UUID;

public interface DeviceTokenIssuer {

  record DeviceIssuedTokens(
      String accessToken,
      String refreshToken,
      long accessTokenExpiresInSeconds,
      long refreshTokenExpiresInSeconds,
      long refreshTokenMaxExpiresInSeconds) {}

  record ParsedDeviceAccessToken(UUID deviceId, Long headquarterId, String scope, String jti) {}

  DeviceIssuedTokens issuePair(PosDevice device);

  DeviceIssuedTokens refresh(String refreshToken);

  ParsedDeviceAccessToken parseAccessToken(String accessToken);

  void revokeRefreshForDevice(UUID deviceId);
}
