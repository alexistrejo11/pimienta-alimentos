package io.github.alexistrejo11.pimienta.module.pos.core.application;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Opaque HQ-scoped sequence cursor: {@code cursor-hq-{id}-s{sequence}}. */
public final class PosSyncCursor {

  private static final Pattern PATTERN = Pattern.compile("^cursor-hq-(\\d+)-s(\\d+)$");

  private final long headquarterId;
  private final long sequence;

  private PosSyncCursor(long headquarterId, long sequence) {
    this.headquarterId = headquarterId;
    this.sequence = sequence;
  }

  public long headquarterId() {
    return headquarterId;
  }

  public long sequence() {
    return sequence;
  }

  public String format() {
    return "cursor-hq-" + headquarterId + "-s" + sequence;
  }

  public static PosSyncCursor of(long headquarterId, long sequence) {
    return new PosSyncCursor(headquarterId, Math.max(0L, sequence));
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
      long sequence = Long.parseLong(m.group(2));
      return Optional.of(of(hqId, sequence));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  public PosSyncCursor advanceTo(long candidateSequence) {
    return of(headquarterId, Math.max(sequence, candidateSequence));
  }
}
