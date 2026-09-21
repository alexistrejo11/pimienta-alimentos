package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.InventoryDashboard;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryGlobalSummary;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryGlobalSummaryRepository;
import java.math.BigDecimal;
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

  @Override
  public InventoryDashboard stockKpis(List<Long> hqs) {
    if (hqs != null && hqs.isEmpty()) {
      return InventoryDashboard.empty();
    }
    InventoryDashboardProjection row = repository.dashboardKpis(hqs);
    if (row == null) {
      return InventoryDashboard.empty();
    }
    return new InventoryDashboard(
        nz(row.getSkuCount()),
        nz(row.getLowStockCount()),
        nz(row.getOutOfStockCount()),
        0,
        nz(row.getTotalAvailableQuantity()),
        row.getTotalStockValue() == null ? BigDecimal.ZERO : row.getTotalStockValue());
  }

  private static long nz(Long value) {
    return value == null ? 0 : value;
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
