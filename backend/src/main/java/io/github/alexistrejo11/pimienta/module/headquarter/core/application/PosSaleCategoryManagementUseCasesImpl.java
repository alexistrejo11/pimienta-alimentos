package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryConflictException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSaleCategoryManagementUseCasesImpl implements PosSaleCategoryManagementUseCases {
  private final HeadquarterRepository headquarters;
  private final PosSaleCategoryRepository categories;

  public PosSaleCategoryManagementUseCasesImpl(HeadquarterRepository headquarters, PosSaleCategoryRepository categories) {
    this.headquarters = headquarters; this.categories = categories;
  }
  public List<PosSaleCategory> list(long hq, boolean includeInactive) { assertHq(hq); return categories.findByHeadquarterId(hq, includeInactive); }
  @Transactional public PosSaleCategory create(long hq, String name, int order) {
    assertHq(hq); String value = name.strip();
    if (categories.findActiveByName(hq, value).isPresent()) throw new PosSaleCategoryConflictException(value);
    return categories.save(PosSaleCategory.create(hq, value, order));
  }
  @Transactional public PosSaleCategory rename(long hq, long id, String name) {
    PosSaleCategory category = get(hq, id); String value = name.strip();
    categories.findActiveByName(hq, value).filter(other -> !other.getId().equals(id)).ifPresent(other -> { throw new PosSaleCategoryConflictException(value); });
    category.rename(value); return categories.save(category);
  }
  @Transactional public PosSaleCategory reorder(long hq, long id, int order) { PosSaleCategory c = get(hq, id); c.reorder(order); return categories.save(c); }
  @Transactional public void archive(long hq, long id) {
    PosSaleCategory c = get(hq, id);
    if (categories.countActiveCatalogItems(hq, id) > 0) throw new PosSaleCategoryConflictException(id);
    c.archive(); c.setDeletedAt(java.time.LocalDateTime.now()); categories.save(c);
  }
  private PosSaleCategory get(long hq, long id) { assertHq(hq); return categories.findById(hq, id).orElseThrow(() -> new PosSaleCategoryNotFoundException(id)); }
  private void assertHq(long hq) { headquarters.findById(hq).orElseThrow(); }
}
