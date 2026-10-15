package io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class OperatingExpenseNotFoundException extends ResourceNotFoundException {

  public OperatingExpenseNotFoundException(long id) {
    super(
        ErrorCode.OPERATING_EXPENSE_NOT_FOUND,
        "The requested expense was not found.",
        Map.of("expenseId", id),
        "OperatingExpense not found: id=" + id);
  }
}
