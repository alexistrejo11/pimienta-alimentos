package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import java.math.BigDecimal;

public interface InventoryDashboardProjection {
  Long getSkuCount();

  Long getLowStockCount();

  Long getOutOfStockCount();

  Long getTotalAvailableQuantity();

  BigDecimal getTotalStockValue();
}
