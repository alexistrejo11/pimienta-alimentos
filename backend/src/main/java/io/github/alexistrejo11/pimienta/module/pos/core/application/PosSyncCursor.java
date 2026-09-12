package io.github.alexistrejo11.pimienta.module.pos.core.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Opaque HQ-scoped watermark: {@code cursor-hq-{id}-v{epochMillis}}. */
public final class PosSyncCursor {

  private static final Pattern PATTERN = Pattern.compile("^cursor-hq-(\\d+)-v(\\d+)$");
  private static final ZoneId ZONE = ZoneId.systemDefault();

  private final long headquarterId;
  private final long watermarkMillis;

  private PosSyncCursor(long headquarterId, long watermarkMillis) {
    this.headquarterId = headquarterId;
    this.watermarkMillis = watermarkMillis;
  }

  public long headquarterId() {
    return headquarterId;
  }

  public long watermarkMillis() {
    return watermarkMillis;
  }

  public LocalDateTime since() {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(watermarkMillis), ZONE);
  }

  public String format() {
    return "cursor-hq-" + headquarterId + "-v" + watermarkMillis;
  }

  public static PosSyncCursor of(long headquarterId, long watermarkMillis) {
    return new PosSyncCursor(headquarterId, Math.max(0L, watermarkMillis));
  }

  public static PosSyncCursor now(long headquarterId) {
    return of(headquarterId, Instant.now().toEpochMilli());
  }

  public static Optional<PosSyncCursor> tryParse(String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    Matcher m = PATTERN.matcher(raw.strip());
    if (!m.matches()) {
      return Optional.empty();
    }
    try {
      long hqId = Long.parseLong(m.group(1));
      long watermark = Long.parseLong(m.group(2));
      return Optional.of(of(hqId, watermark));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  public static long toMillis(LocalDateTime dateTime) {
    if (dateTime == null) {
      return 0L;
    }
    return dateTime.atZone(ZONE).toInstant().toEpochMilli();
  }

  public PosSyncCursor advanceTo(long candidateMillis) {
    return of(headquarterId, Math.max(watermarkMillis, candidateMillis));
  }
}
