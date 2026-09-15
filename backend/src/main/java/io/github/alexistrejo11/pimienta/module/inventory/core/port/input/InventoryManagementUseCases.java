package io.github.alexistrejo11.pimienta.module.inventory.core.port.input;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventorySearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;

public interface InventoryManagementUseCases {

  Page<Inventory> search(InventorySearchCriteria criteria, Pageable pageable);

  Inventory getById(Long id);

  List<Inventory> findByItemId(Long itemId);

  List<Inventory> findByLocationId(Long locationId);

  Page<Inventory> findLowStock(InventorySearchCriteria criteria, Pageable pageable);

  Page<Inventory> findOutOfStock(InventorySearchCriteria criteria, Pageable pageable);

  Inventory createInitialStock(long itemId, long locationId, int initialQuantity);

  Page<InventoryGlobalSummary> searchGlobalSummary(String search, io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory category, Inventory.InventoryStatus status, java.math.BigDecimal minCost, java.math.BigDecimal maxCost, java.util.List<Long> headquarterIds, Pageable pageable);
}
