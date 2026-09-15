package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import java.math.BigDecimal;

public interface InventoryGlobalSummaryProjection {
  Long getItemId(); String getSku(); String getName(); String getCategory(); Long getHeadquarterId();
  String getHeadquarterName(); Long getAvailableQuantity(); Long getReservedQuantity(); Long getInTransitQuantity();
  Long getTotalQuantity(); BigDecimal getUnitCost(); BigDecimal getTotalValue(); String getStatus();
}
