package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.security;

import io.github.alexistrejo11.pimienta.config.security.DeviceJwtProperties;
import io.github.alexistrejo11.pimienta.config.security.JwtProperties;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.InvalidDeviceRefreshTokenException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceRefreshTokenStore;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.shared.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class DeviceJwtTokenService implements DeviceTokenIssuer {

  public static final String TYP_DEVICE = "device";
  public static final String SCOPE_POS_SYNC = "pos:sync";

  private final JwtProperties jwtProperties;
  private final DeviceJwtProperties deviceJwtProperties;
  private final DeviceRefreshTokenStore refreshTokenStore;
  private final PosDeviceRepository deviceRepository;

  public DeviceJwtTokenService(
      JwtProperties jwtProperties,
      DeviceJwtProperties deviceJwtProperties,
      DeviceRefreshTokenStore refreshTokenStore,
      PosDeviceRepository deviceRepository) {
    this.jwtProperties = jwtProperties;
    this.deviceJwtProperties = deviceJwtProperties;
    this.refreshTokenStore = refreshTokenStore;
    this.deviceRepository = deviceRepository;
  }

  @Override
  public DeviceIssuedTokens issuePair(PosDevice device) {
    Instant now = Instant.now();
    Duration accessTtl = accessTtl();
    Duration refreshTtl = refreshTtl();

    String access =
        buildAccessToken(device.getId(), device.getHeadquarterId(), UUID.randomUUID().toString(), now, accessTtl);
    String refresh = UUID.randomUUID() + "." + UUID.randomUUID();
    refreshTokenStore.remember(sha256(refresh), device.getId(), refreshTtl);

    return new DeviceIssuedTokens(
        access,
        refresh,
        accessTtl.getSeconds(),
        refreshTtl.getSeconds(),
        maxRefreshTtl().getSeconds());
  }

  @Override
  public DeviceIssuedTokens refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw invalidRefresh("Missing refresh token.");
    }
    String hash = sha256(refreshToken);
    UUID deviceId =
        refreshTokenStore
            .findDeviceId(hash)
            .orElseThrow(() -> invalidRefresh("Refresh token revoked or unknown."));
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      refreshTokenStore.remove(hash);
      throw new PosDeviceRevokedException(deviceId);
    }
    refreshTokenStore.remove(hash);
    return issuePair(device);
  }

  @Override
  public ParsedDeviceAccessToken parseAccessToken(String accessToken) {
    try {
      Claims claims = parseClaims(accessToken);
      if (!TYP_DEVICE.equals(claims.get("typ"))) {
        throw invalidAccess("Not a device access token.");
      }
      String scope = claims.get("scope", String.class);
      if (!SCOPE_POS_SYNC.equals(scope)) {
        throw invalidAccess("Invalid device scope.");
      }
      UUID deviceId = UUID.fromString(claims.get("deviceId", String.class));
      Long headquarterId = Long.parseLong(claims.get("headquarterId", String.class));
      return new ParsedDeviceAccessToken(deviceId, headquarterId, scope, claims.getId());
    } catch (ExpiredJwtException e) {
      throw invalidAccess("Device access token expired.", e);
    } catch (JwtException | IllegalArgumentException e) {
      throw invalidAccess("Invalid device access token.", e);
    }
  }

  @Override
  public void revokeRefreshForDevice(UUID deviceId) {
    refreshTokenStore.removeByDeviceId(deviceId);
  }

  private String buildAccessToken(
      UUID deviceId, Long headquarterId, String jti, Instant now, Duration accessTtl) {
    return Jwts.builder()
        .id(jti)
        .subject(deviceId.toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTtl)))
        .claim("typ", TYP_DEVICE)
        .claim("scope", SCOPE_POS_SYNC)
        .claim("deviceId", deviceId.toString())
        .claim("headquarterId", String.valueOf(headquarterId))
        .signWith(signingKey())
        .compact();
  }

  private Duration accessTtl() {
    return Duration.ofMinutes(deviceJwtProperties.getAccessTokenTtlMinutes());
  }

  private Duration refreshTtl() {
    int days =
        Math.min(
            deviceJwtProperties.getRefreshTokenTtlDays(),
            deviceJwtProperties.getRefreshTokenMaxTtlDays());
    return Duration.ofDays(days);
  }

  private Duration maxRefreshTtl() {
    return Duration.ofDays(deviceJwtProperties.getRefreshTokenMaxTtlDays());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey signingKey() {
    byte[] bytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }

  static String sha256(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static AuthenticationException invalidAccess(String message) {
    return new AuthenticationException(message, Map.of(), message, null);
  }

  private static AuthenticationException invalidAccess(String message, Throwable cause) {
    return new AuthenticationException(message, Map.of(), message, cause);
  }

  private static InvalidDeviceRefreshTokenException invalidRefresh(String message) {
    return new InvalidDeviceRefreshTokenException(message);
  }
}
