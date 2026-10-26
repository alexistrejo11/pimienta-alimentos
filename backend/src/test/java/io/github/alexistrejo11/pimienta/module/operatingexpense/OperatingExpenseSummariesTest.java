package io.github.alexistrejo11.pimienta.module.operatingexpense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.OperatingExpenseSummaries;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperatingExpenseSummariesTest {

  @Test
  void emptySelectionHasNoTopCategory() {
    assertNull(OperatingExpenseSummaries.topCategory(List.of()));
    assertNull(OperatingExpenseSummaries.topCategory(null));
  }

  @Test
  void higherCountBreaksAnAmountTie() {
    OperatingExpenseCategoryTotal top =
        OperatingExpenseSummaries.topCategory(
            List.of(
                total(OperatingExpenseCategory.RENT, "30.00", 1),
                total(OperatingExpenseCategory.SUPPLIERS_MERCHANDISE, "30.00", 2)));

    assertEquals(OperatingExpenseCategory.SUPPLIERS_MERCHANDISE, top.category());
  }

  @Test
  void categoryNameBreaksAFullTie() {
    OperatingExpenseCategoryTotal top =
        OperatingExpenseSummaries.topCategory(
            List.of(
                total(OperatingExpenseCategory.SUPPLIERS_MERCHANDISE, "30.00", 1),
                total(OperatingExpenseCategory.RENT, "30.0", 1)));

    assertEquals(OperatingExpenseCategory.RENT, top.category());
  }

  private static OperatingExpenseCategoryTotal total(OperatingExpenseCategory category, String amount, long count) {
    return new OperatingExpenseCategoryTotal(category, new BigDecimal(amount), count);
  }
}
