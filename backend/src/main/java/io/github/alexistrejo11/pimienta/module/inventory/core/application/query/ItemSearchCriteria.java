package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;

public record ItemSearchCriteria(String search, Item.ItemCategory category, Item.ItemStatus status, Item.CatalogRole catalogRole, java.math.BigDecimal minCost, java.math.BigDecimal maxCost) {

  public static ItemSearchCriteria empty() {
    return new ItemSearchCriteria(null, null, null, null, null, null);
  }
}
