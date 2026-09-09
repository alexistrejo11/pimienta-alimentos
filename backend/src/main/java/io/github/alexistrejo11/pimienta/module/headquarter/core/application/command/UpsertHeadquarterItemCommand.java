package io.github.alexistrejo11.pimienta.module.headquarter.core.application.command;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import java.math.BigDecimal;

public record UpsertHeadquarterItemCommand(
    String saleCategory,
    BigDecimal salePrice,
    Boolean available,
    StockPolicy stockPolicy,
    Integer negativeStockLimit) {}
