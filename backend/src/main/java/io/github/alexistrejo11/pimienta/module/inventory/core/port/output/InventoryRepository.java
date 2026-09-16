package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventorySearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryRepository {

  Optional<Inventory> findById(long id);

  Optional<Inventory> findByItemIdAndLocationId(long itemId, long locationId);

  Optional<Inventory> findByItemIdAndLocationIdForUpdate(long itemId, long locationId);

  Page<Inventory> search(InventorySearchCriteria criteria, Pageable pageable);

  List<Inventory> findByItemId(long itemId);

  List<Inventory> findByLocationId(long locationId);

  Page<Inventory> findLowStock(InventorySearchCriteria criteria, Pageable pageable);

  Page<Inventory> findOutOfStock(InventorySearchCriteria criteria, Pageable pageable);

  long countByLocationId(long locationId);

  Inventory save(Inventory inventory);
}
