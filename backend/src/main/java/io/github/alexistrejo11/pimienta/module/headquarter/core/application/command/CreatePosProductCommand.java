package io.github.alexistrejo11.pimienta.module.headquarter.core.application.command;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import java.math.BigDecimal;

public record CreatePosProductCommand(
    String name, String description, BigDecimal costPrice, BigDecimal salePrice,
    ItemCategory category, ItemUnit unit, String brand, String barcode,
    int reorderPoint, int reorderQuantity, long posSaleCategoryId,
    Boolean available, StockPolicy stockPolicy, Integer negativeStockLimit) {}
