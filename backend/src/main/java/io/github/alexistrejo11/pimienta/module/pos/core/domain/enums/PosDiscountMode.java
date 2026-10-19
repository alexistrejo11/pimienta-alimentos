package io.github.alexistrejo11.pimienta.module.pos.core.domain.enums;

/** How a sale-level discount was chosen. {@link #UNDEFINED} means the ticket has no discount. */
public enum PosDiscountMode {
  UNDEFINED,
  FULL_COURTESY,
  AT_COST,
  TARGET_AMOUNT;

  public static PosDiscountMode fromWire(String raw) {
    if (raw == null || raw.isBlank()) {
      return UNDEFINED;
    }
    try {
      PosDiscountMode mode = valueOf(raw.strip());
      return mode == UNDEFINED ? UNDEFINED : mode;
    } catch (IllegalArgumentException ex) {
      return UNDEFINED;
    }
  }
}
