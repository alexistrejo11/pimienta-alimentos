package io.github.alexistrejo11.pimienta.config.web;

import java.time.ZoneId;

/** Request zone bound by {@link ClientTimeZoneFilter}. Safe off a request thread. */
public final class ClientTimeZones {

  private static final ThreadLocal<ZoneId> CURRENT = new ThreadLocal<>();

  private ClientTimeZones() {}

  public static ZoneId current() {
    ZoneId zone = CURRENT.get();
    return zone != null ? zone : ClientTimeZone.FALLBACK;
  }

  static void bind(ZoneId zone) {
    CURRENT.set(zone);
  }

  static void clear() {
    CURRENT.remove();
  }
}
