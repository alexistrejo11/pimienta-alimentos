package io.github.alexistrejo11.pimienta.shared.util;

/**
 * Shared string normalization. Blank handling is Unicode-aware ({@code isBlank} + {@code strip}).
 */
public final class Strings {

  private Strings() {}

  /** {@code null} or blank becomes {@code null}; otherwise {@link String#strip()}. */
  public static String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s.strip();
  }

  /**
   * {@code null} or blank (including whitespace-only) becomes {@code ""}; otherwise {@link String#strip()}.
   */
  public static String nullToEmpty(String s) {
    return (s == null || s.isBlank()) ? "" : s.strip();
  }
}
