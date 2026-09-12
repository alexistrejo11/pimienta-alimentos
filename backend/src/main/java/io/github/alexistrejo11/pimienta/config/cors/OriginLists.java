package io.github.alexistrejo11.pimienta.config.cors;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Splits CORS origin settings that arrive as a single comma-separated value (typical for {@code
 * PIMIENTA_CORS_ALLOWED_ORIGINS} interpolated into YAML) into distinct origins.
 */
final class OriginLists {

  private OriginLists() {}

  static List<String> parse(List<String> raw) {
    if (raw == null || raw.isEmpty()) {
      return List.of();
    }
    Set<String> origins = new LinkedHashSet<>();
    for (String entry : raw) {
      if (entry == null || entry.isBlank()) {
        continue;
      }
      for (String token : entry.split(",")) {
        String origin = stripQuotes(token.trim());
        if (!origin.isEmpty()) {
          origins.add(origin);
        }
      }
    }
    return new ArrayList<>(origins);
  }

  private static String stripQuotes(String value) {
    if (value.length() >= 2
        && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
      return value.substring(1, value.length() - 1).trim();
    }
    return value;
  }
}
