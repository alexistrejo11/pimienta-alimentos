package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Amount and count for one business day inside a summary filter. */
public record OperatingExpenseDayTotal(LocalDate occurredOn, BigDecimal totalAmount, long count) {}
