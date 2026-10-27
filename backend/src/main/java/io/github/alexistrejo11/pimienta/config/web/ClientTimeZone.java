package io.github.alexistrejo11.pimienta.config.web;

import java.time.DateTimeException;
import java.time.ZoneId;

/** {@code X-Timezone} is an IANA id or offset, never a local timestamp. */
public final class ClientTimeZone {

  public static final String HEADER = "X-Timezone";

  /** Used when the header is absent or blank, and on threads with no request. */
  public static final ZoneId FALLBACK = ZoneId.of("America/Mexico_City");

  private ClientTimeZone() {}

  /**
   * Blank values use {@link #FALLBACK}. A value {@link ZoneId#of(String)} cannot parse throws
   * {@link DateTimeException}.
   */
  public static ZoneId parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return FALLBACK;
    }
    return ZoneId.of(raw.trim());
  }
}
