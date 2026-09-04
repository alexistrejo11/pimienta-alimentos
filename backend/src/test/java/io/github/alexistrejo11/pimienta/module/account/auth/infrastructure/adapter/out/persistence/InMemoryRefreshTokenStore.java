package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.RefreshTokenStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Test-only refresh-token store. CI and local tests must not require Redis. */
@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

  private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

  @Override
  public void remember(String jti, Long userId, Duration ttl) {
    tokens.put(jti, new Entry(userId, Instant.now().plus(ttl)));
  }

  @Override
  public Optional<Long> findUserId(String jti) {
    Entry entry = tokens.get(jti);
    if (entry == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(entry.expiresAt())) {
      tokens.remove(jti, entry);
      return Optional.empty();
    }
    return Optional.of(entry.userId());
  }

  @Override
  public void remove(String jti) {
    tokens.remove(jti);
  }

  private record Entry(Long userId, Instant expiresAt) {}
}
