package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import java.util.Comparator;
import java.util.List;

/** Picks the leading category for an expense summary. */
public final class OperatingExpenseSummaries {

  private OperatingExpenseSummaries() {}

  /**
   * Highest amount, then highest count, then category name. Null when {@code rows} is empty.
   */
  public static OperatingExpenseCategoryTotal topCategory(List<OperatingExpenseCategoryTotal> rows) {
    if (rows == null || rows.isEmpty()) {
      return null;
    }
    Comparator<OperatingExpenseCategoryTotal> ranking =
        Comparator.comparing(
                OperatingExpenseCategoryTotal::totalAmount, Comparator.nullsFirst(Comparator.naturalOrder()))
            .reversed()
            .thenComparing(Comparator.comparingLong(OperatingExpenseCategoryTotal::count).reversed())
            .thenComparing(row -> row.category().name());
    return rows.stream().min(ranking).orElse(null);
  }
}
