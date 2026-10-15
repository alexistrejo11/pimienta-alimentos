package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class OperatingExpenseObjectKeys {

  private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

  private OperatingExpenseObjectKeys() {}

  public static String build(String folder, long headquarterId, LocalDate occurredOn, UUID id) {
    String relative =
        "operating-expenses/" + headquarterId + "/" + occurredOn.format(YEAR_MONTH) + "/" + id + ".jpg";
    if (folder == null || folder.isBlank()) {
      return relative;
    }
    String prefix = folder.strip().replaceAll("/+$", "");
    return prefix.isEmpty() ? relative : prefix + "/" + relative;
  }
}
