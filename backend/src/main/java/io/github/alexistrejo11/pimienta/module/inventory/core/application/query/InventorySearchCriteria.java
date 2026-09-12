package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;

public record InventorySearchCriteria(
    Long itemId, Long locationId, InventoryStatus status, Long headquarterId) {

  public static InventorySearchCriteria empty() {
    return new InventorySearchCriteria(null, null, null, null);
  }
}
