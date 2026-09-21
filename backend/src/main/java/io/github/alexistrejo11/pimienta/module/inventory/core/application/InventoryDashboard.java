package io.github.alexistrejo11.pimienta.module.inventory.core.application;

import java.math.BigDecimal;

public record InventoryDashboard(
    long skuCount,
    long lowStockCount,
    long outOfStockCount,
    long openCountSessionCount,
    long totalAvailableQuantity,
    BigDecimal totalStockValue) {

  public static InventoryDashboard empty() {
    return new InventoryDashboard(0, 0, 0, 0, 0, BigDecimal.ZERO);
  }
}
