package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import java.math.BigDecimal;

/** Amount and count for one category inside a summary filter. */
public record OperatingExpenseCategoryTotal(
    OperatingExpenseCategory category, BigDecimal totalAmount, long count) {}
