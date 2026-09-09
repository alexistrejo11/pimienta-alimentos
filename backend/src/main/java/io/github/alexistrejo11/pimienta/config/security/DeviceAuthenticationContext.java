package io.github.alexistrejo11.pimienta.config.security;

import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.ParsedDeviceAccessToken;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/** Security principal for POS device access tokens (`typ=device`). */
public final class DeviceAuthenticationContext implements Principal, Serializable {

  private static final long serialVersionUID = 1L;

  public static final String SCOPE_POS_SYNC = "pos:sync";
  public static final String AUTHORITY_SCOPE_POS_SYNC = "SCOPE_pos:sync";

  private final ParsedDeviceAccessToken accessToken;

  public DeviceAuthenticationContext(ParsedDeviceAccessToken accessToken) {
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
  }

  public ParsedDeviceAccessToken accessToken() {
    return accessToken;
  }

  public UUID deviceId() {
    return accessToken.deviceId();
  }

  public Long headquarterId() {
    return accessToken.headquarterId();
  }

  @Override
  public String getName() {
    return accessToken.deviceId().toString();
  }

  @Override
  public String toString() {
    return "DeviceAuthenticationContext(deviceId=" + accessToken.deviceId() + ")";
  }
}
