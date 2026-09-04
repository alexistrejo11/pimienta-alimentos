package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.RefreshTokenStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RedisRefreshTokenStore implements RefreshTokenStore {

  private static final String PREFIX = "pimienta:rt:";

  private final StringRedisTemplate redis;

  public RedisRefreshTokenStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public void remember(String jti, Long userId, Duration ttl) {
    redis.opsForValue().set(PREFIX + jti, userId.toString(), ttl);
  }

  @Override
  public Optional<Long> findUserId(String jti) {
    String v = redis.opsForValue().get(PREFIX + jti);
    if (v == null || v.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Long.parseLong(v));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  @Override
  public void remove(String jti) {
    redis.delete(PREFIX + jti);
  }
}
