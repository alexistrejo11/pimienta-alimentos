package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import java.util.List;

public interface PosSaleCategoryManagementUseCases {
  List<PosSaleCategory> list(long headquarterId, boolean includeInactive);
  PosSaleCategory create(long headquarterId, String name, int displayOrder);
  PosSaleCategory rename(long headquarterId, long id, String name);
  PosSaleCategory reorder(long headquarterId, long id, int displayOrder);
  void archive(long headquarterId, long id);
}
