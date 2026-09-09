package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/** Stores hashed device refresh tokens (never plaintext). */
public interface DeviceRefreshTokenStore {

  void remember(String tokenHash, UUID deviceId, Duration ttl);

  Optional<UUID> findDeviceId(String tokenHash);

  void remove(String tokenHash);

  /** Remove the currently tracked refresh hash for a device (revoke / rotate). */
  void removeByDeviceId(UUID deviceId);
}
