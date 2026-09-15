package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryGlobalSummaryRepository {
  Page<InventoryGlobalSummary> search(String search, ItemCategory category, InventoryStatus status, java.math.BigDecimal minCost, java.math.BigDecimal maxCost, List<Long> headquarterIds, Pageable pageable);
}
