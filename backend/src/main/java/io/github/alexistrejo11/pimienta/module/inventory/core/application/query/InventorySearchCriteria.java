package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import java.util.List;

public record InventorySearchCriteria(
    Long itemId, Long locationId, InventoryStatus status, List<Long> headquarterIds) {

  public static InventorySearchCriteria empty() {
    return new InventorySearchCriteria(null, null, null, null);
  }
}
