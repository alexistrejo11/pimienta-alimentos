package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Stores hashed device refresh tokens (never plaintext). */
public interface DeviceRefreshTokenStore {

  void remember(String tokenHash, UUID deviceId, Duration ttl);

  Optional<UUID> findDeviceId(String tokenHash);

  /** Atomically removes the token and returns its device, so only one caller can rotate it. */
  Optional<UUID> consume(String tokenHash);

  void remove(String tokenHash);

  /** Remove the currently tracked refresh hash for a device (revoke / rotate). */
  void removeByDeviceId(UUID deviceId);

  /**
   * Keeps the pair issued when {@code previousTokenHash} was rotated, so a concurrent or retried
   * refresh with the same token gets the same pair instead of a 401. Contains plaintext tokens;
   * keep {@code grace} short.
   */
  void rememberRotation(String previousTokenHash, String rotatedPayload, Duration grace);

  Optional<String> findRotation(String previousTokenHash);
}
