package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import java.math.BigDecimal;

/** How an item is stocked. Applies to warehouse items and product-backed items alike. */
public record UpdateItemStockSettingsCommand(
    BigDecimal costPrice, int reorderPoint, int reorderQuantity, ItemStatus status) {}
