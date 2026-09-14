package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryCountRepository {

  InventoryCountSession save(InventoryCountSession session);

  Optional<InventoryCountSession> findById(long id);

  Page<InventoryCountSession> search(InventoryCountSearchCriteria criteria, Pageable pageable);

  boolean existsActiveByLocationId(long locationId);
}
