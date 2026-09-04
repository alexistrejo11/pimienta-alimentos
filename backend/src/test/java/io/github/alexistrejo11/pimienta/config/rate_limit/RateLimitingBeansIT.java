package io.github.alexistrejo11.pimienta.config.rate_limit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.RefreshTokenStore;
import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.out.persistence.InMemoryRefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tests never connect to Redis; rate limiting is off so CI does not need a local instance. */
@SpringBootTest
@ActiveProfiles("test")
class RateLimitingBeansIT {

  @Autowired ApplicationContext applicationContext;
  @Autowired Environment environment;
  @Autowired RefreshTokenStore refreshTokenStore;

  @Test
  void rateLimitingIsDisabledInTests() {
    assertThat(environment.getProperty("pimienta.rate-limiting.enabled")).isEqualTo("false");
    assertThat(applicationContext.getBeanNamesForType(RedisTokenBucketRateLimiter.class)).isEmpty();
    assertThat(applicationContext.containsBean("globalRateLimitFilterRegistration")).isFalse();
  }

  @Test
  void redisTemplateIsNotRegistered() {
    assertThat(applicationContext.getBeanNamesForType(StringRedisTemplate.class)).isEmpty();
  }

  @Test
  void refreshTokensUseInMemoryStore() {
    assertThat(refreshTokenStore).isInstanceOf(InMemoryRefreshTokenStore.class);
  }
}
