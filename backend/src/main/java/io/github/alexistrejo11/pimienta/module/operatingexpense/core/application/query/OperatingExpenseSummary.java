package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate for the same filters as the expense list. {@code topCategory} is null when nothing
 * matches. {@code byDay} omits days with no expenses and is ordered by {@code occurredOn}.
 */
public record OperatingExpenseSummary(
    BigDecimal totalAmount, OperatingExpenseCategoryTotal topCategory, List<OperatingExpenseDayTotal> byDay) {}
