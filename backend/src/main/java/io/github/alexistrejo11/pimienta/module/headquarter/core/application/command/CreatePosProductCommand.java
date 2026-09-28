package io.github.alexistrejo11.pimienta.module.headquarter.core.application.command;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import java.math.BigDecimal;

public record CreatePosProductCommand(
    String name,
    String description,
    String barcode,
    Unit unit,
    BigDecimal salePrice,
    long posSaleCategoryId,
    Boolean available,
    StockPolicy stockPolicy,
    Integer negativeStockLimit) {}
