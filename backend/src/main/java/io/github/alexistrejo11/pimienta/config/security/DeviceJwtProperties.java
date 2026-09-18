package io.github.alexistrejo11.pimienta.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pimienta.security.device-jwt")
public class DeviceJwtProperties {

  private int accessTokenTtlMinutes = 15;
  private int refreshTokenTtlDays = 180;
  private int refreshTokenMaxTtlDays = 365;

  public int getAccessTokenTtlMinutes() {
    return accessTokenTtlMinutes;
  }

  public void setAccessTokenTtlMinutes(int accessTokenTtlMinutes) {
    this.accessTokenTtlMinutes = accessTokenTtlMinutes;
  }

  public int getRefreshTokenTtlDays() {
    return refreshTokenTtlDays;
  }

  public void setRefreshTokenTtlDays(int refreshTokenTtlDays) {
    this.refreshTokenTtlDays = refreshTokenTtlDays;
  }

  public int getRefreshTokenMaxTtlDays() {
    return refreshTokenMaxTtlDays;
  }

  public void setRefreshTokenMaxTtlDays(int refreshTokenMaxTtlDays) {
    this.refreshTokenMaxTtlDays = refreshTokenMaxTtlDays;
  }
}
