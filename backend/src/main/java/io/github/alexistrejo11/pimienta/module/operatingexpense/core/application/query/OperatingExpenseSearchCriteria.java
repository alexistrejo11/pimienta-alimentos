package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import java.time.LocalDate;

public record OperatingExpenseSearchCriteria(
    String search, Long headquarterId, LocalDate from, LocalDate to, OperatingExpenseCategory category) {}
