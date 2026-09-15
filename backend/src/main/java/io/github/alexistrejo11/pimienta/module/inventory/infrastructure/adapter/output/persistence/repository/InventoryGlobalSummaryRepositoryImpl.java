package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryGlobalSummaryRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryGlobalSummaryRepositoryImpl implements InventoryGlobalSummaryRepository {
  private final InventoryJpaRepository repository;
  public InventoryGlobalSummaryRepositoryImpl(InventoryJpaRepository repository) { this.repository = repository; }
  public Page<InventoryGlobalSummary> search(String search, ItemCategory category, InventoryStatus status, java.math.BigDecimal minCost, java.math.BigDecimal maxCost, List<Long> hqs, Pageable pageable) {
    return repository.searchGlobalSummary(blankToNull(search), category == null ? null : category.name(), status == null ? null : status.name(), minCost, maxCost, hqs, pageable).map(row -> new InventoryGlobalSummary(row.getItemId(), row.getSku(), row.getName(), ItemCategory.valueOf(row.getCategory()), row.getHeadquarterId(), row.getHeadquarterName(), row.getAvailableQuantity(), row.getReservedQuantity(), row.getInTransitQuantity(), row.getTotalQuantity(), row.getUnitCost(), row.getTotalValue(), InventoryStatus.valueOf(row.getStatus())));
  }
  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
