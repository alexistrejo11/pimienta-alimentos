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
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class DeviceJwtTokenService implements DeviceTokenIssuer {

  public static final String TYP_DEVICE = "device";
  public static final String SCOPE_POS_SYNC = "pos:sync";
  private static final int ROTATION_WAIT_ATTEMPTS = 10;
  private static final long ROTATION_WAIT_MILLIS = 100;

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
    Optional<UUID> claimed = refreshTokenStore.consume(hash);
    if (claimed.isEmpty()) {
      return replayRotation(hash);
    }
    UUID deviceId = claimed.get();
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }
    DeviceIssuedTokens issued = issuePair(device);
    refreshTokenStore.rememberRotation(hash, encodeRotation(deviceId, issued), refreshReuseGrace());
    return issued;
  }

  /**
   * Returns the pair already issued for a token rotated moments ago. Covers tablets that fire two
   * refreshes at once or lose the response to a timeout; outside the grace window it is a 401.
   */
  private DeviceIssuedTokens replayRotation(String hash) {
    Optional<String> payload = refreshTokenStore.findRotation(hash);
    for (int attempt = 0; payload.isEmpty() && attempt < ROTATION_WAIT_ATTEMPTS; attempt++) {
      sleepQuietly();
      payload = refreshTokenStore.findRotation(hash);
    }
    String[] parts =
        payload.orElseThrow(() -> invalidRefresh("Refresh token revoked or unknown.")).split("\\|", -1);
    UUID deviceId = UUID.fromString(parts[0]);
    DeviceIssuedTokens issued =
        new DeviceIssuedTokens(
            parts[1], parts[2], Long.parseLong(parts[3]), Long.parseLong(parts[4]), Long.parseLong(parts[5]));
    if (refreshTokenStore.findDeviceId(sha256(issued.refreshToken())).filter(deviceId::equals).isEmpty()) {
      throw invalidRefresh("Refresh token revoked or unknown.");
    }
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }
    return issued;
  }

  private static String encodeRotation(UUID deviceId, DeviceIssuedTokens issued) {
    return String.join(
        "|",
        deviceId.toString(),
        issued.accessToken(),
        issued.refreshToken(),
        String.valueOf(issued.accessTokenExpiresInSeconds()),
        String.valueOf(issued.refreshTokenExpiresInSeconds()),
        String.valueOf(issued.refreshTokenMaxExpiresInSeconds()));
  }

  private static void sleepQuietly() {
    try {
      Thread.sleep(ROTATION_WAIT_MILLIS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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

  private Duration refreshReuseGrace() {
    return Duration.ofSeconds(deviceJwtProperties.getRefreshReuseGraceSeconds());
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
