package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.security;

import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceRefreshTokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryDeviceRefreshTokenStore implements DeviceRefreshTokenStore {

  private final ConcurrentHashMap<String, Entry> byHash = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, String> byDevice = new ConcurrentHashMap<>();

  @Override
  public void remember(String tokenHash, UUID deviceId, Duration ttl) {
    removeByDeviceId(deviceId);
    byHash.put(tokenHash, new Entry(deviceId, Instant.now().plus(ttl)));
    byDevice.put(deviceId, tokenHash);
  }

  @Override
  public Optional<UUID> findDeviceId(String tokenHash) {
    Entry entry = byHash.get(tokenHash);
    if (entry == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(entry.expiresAt())) {
      byHash.remove(tokenHash, entry);
      byDevice.remove(entry.deviceId(), tokenHash);
      return Optional.empty();
    }
    return Optional.of(entry.deviceId());
  }

  @Override
  public void remove(String tokenHash) {
    Entry entry = byHash.remove(tokenHash);
    if (entry != null) {
      byDevice.remove(entry.deviceId(), tokenHash);
    }
  }

  @Override
  public void removeByDeviceId(UUID deviceId) {
    String hash = byDevice.remove(deviceId);
    if (hash != null) {
      byHash.remove(hash);
    }
  }

  private record Entry(UUID deviceId, Instant expiresAt) {}
}
