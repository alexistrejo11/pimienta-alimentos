package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemKind;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import java.math.BigDecimal;

public record ItemSearchCriteria(
    String search,
    ItemCategory category,
    ItemStatus status,
    BigDecimal minCost,
    BigDecimal maxCost,
    ItemKind kind) {

  public static ItemSearchCriteria empty() {
    return new ItemSearchCriteria(null, null, null, null, null, null);
  }
}
