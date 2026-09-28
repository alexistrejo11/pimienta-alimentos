package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import java.util.List;

public interface PosSaleCategoryManagementUseCases {
  List<PosSaleCategory> list(long headquarterId, boolean includeInactive);
  PosSaleCategory create(long headquarterId, String name);
  PosSaleCategory rename(long headquarterId, long id, String name);
  void archive(long headquarterId, long id);
}
