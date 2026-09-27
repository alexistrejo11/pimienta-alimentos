package io.github.alexistrejo11.pimienta.module.headquarter.core.application.command;

import java.util.List;

public record UpsertPosSettingsCommand(
    String currency,
    Integer catalogStaleWarnHours,
    Integer catalogStaleBlockHours,
    List<String> openAmountCategories,
    Boolean allowOpenProducts,
    Integer defaultNegativeStockLimit,
    Boolean stockless) {}
