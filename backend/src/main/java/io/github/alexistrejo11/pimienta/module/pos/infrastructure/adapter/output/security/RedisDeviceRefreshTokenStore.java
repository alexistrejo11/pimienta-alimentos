package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.security;

import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceRefreshTokenStore;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisDeviceRefreshTokenStore implements DeviceRefreshTokenStore {

  private static final String TOKEN_PREFIX = "pimienta:pos:rt:";
  private static final String DEVICE_PREFIX = "pimienta:pos:rt-device:";

  private final StringRedisTemplate redis;

  public RedisDeviceRefreshTokenStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public void remember(String tokenHash, UUID deviceId, Duration ttl) {
    removeByDeviceId(deviceId);
    redis.opsForValue().set(TOKEN_PREFIX + tokenHash, deviceId.toString(), ttl);
    redis.opsForValue().set(DEVICE_PREFIX + deviceId, tokenHash, ttl);
  }

  @Override
  public Optional<UUID> findDeviceId(String tokenHash) {
    String v = redis.opsForValue().get(TOKEN_PREFIX + tokenHash);
    if (v == null || v.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(v));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Override
  public void remove(String tokenHash) {
    String deviceId = redis.opsForValue().get(TOKEN_PREFIX + tokenHash);
    redis.delete(TOKEN_PREFIX + tokenHash);
    if (deviceId != null && !deviceId.isBlank()) {
      String current = redis.opsForValue().get(DEVICE_PREFIX + deviceId);
      if (tokenHash.equals(current)) {
        redis.delete(DEVICE_PREFIX + deviceId);
      }
    }
  }

  @Override
  public void removeByDeviceId(UUID deviceId) {
    String hash = redis.opsForValue().get(DEVICE_PREFIX + deviceId);
    if (hash != null && !hash.isBlank()) {
      redis.delete(TOKEN_PREFIX + hash);
    }
    redis.delete(DEVICE_PREFIX + deviceId);
  }
}
