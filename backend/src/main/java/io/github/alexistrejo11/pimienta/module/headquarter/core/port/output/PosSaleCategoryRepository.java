package io.github.alexistrejo11.pimienta.module.headquarter.core.port.output;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import java.util.List;
import java.util.Optional;

public interface PosSaleCategoryRepository {
  List<PosSaleCategory> findByHeadquarterId(long headquarterId, boolean includeInactive);
  Optional<PosSaleCategory> findById(long headquarterId, long id);
  Optional<PosSaleCategory> findActiveByName(long headquarterId, String name);
  long countActiveCatalogItems(long headquarterId, long categoryId);
  PosSaleCategory save(PosSaleCategory category);
}
