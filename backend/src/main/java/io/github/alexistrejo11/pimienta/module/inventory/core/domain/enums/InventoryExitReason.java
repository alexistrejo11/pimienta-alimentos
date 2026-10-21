package io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums;

/** Why stock left, or why a POS sale was reversed. */
public enum InventoryExitReason {
  /** Automated deduction from a confirmed POS sale. */
  SALE,
  /** Damaged, expired, or otherwise unsellable goods. */
  WASTE,
  /** Source-only exit, such as stock taken to another branch. */
  WITHDRAWAL,
  /** Compensating entry when a POS sale is cancelled. */
  RETURN_FROM_CLIENT
}
