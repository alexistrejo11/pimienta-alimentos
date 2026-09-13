package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.bootstrap;

import io.github.alexistrejo11.pimienta.module.account.user.core.port.input.AdminBootstrapUseCases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Runs after Flyway / Hibernate schema setup. Concurrent replicas racing on an empty DB are treated
 * as a skip (unique email).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "pimienta.bootstrap.admin", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

  private final AdminBootstrapUseCases adminBootstrapUseCases;

  public AdminBootstrapRunner(AdminBootstrapUseCases adminBootstrapUseCases) {
    this.adminBootstrapUseCases = adminBootstrapUseCases;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      adminBootstrapUseCases.ensureInitialAdmin();
    } catch (DataIntegrityViolationException ex) {
      log.info("Admin bootstrap skipped: account was created concurrently");
    }
  }
}
