package io.github.alexistrejo11.pimienta.module.account.user.core.port.input;

/**
 * Creates the first ACTIVE ADMIN from env credentials when the database has none.
 */
public interface AdminBootstrapUseCases {

  /**
   * @return {@code true} if a new admin was persisted
   */
  boolean ensureInitialAdmin();
}
