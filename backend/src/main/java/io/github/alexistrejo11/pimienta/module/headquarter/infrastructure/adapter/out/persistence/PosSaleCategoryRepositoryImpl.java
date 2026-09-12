package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.PosSaleCategorySpringDataRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PosSaleCategoryRepositoryImpl implements PosSaleCategoryRepository {
  private final PosSaleCategorySpringDataRepository repository;
  public PosSaleCategoryRepositoryImpl(PosSaleCategorySpringDataRepository repository) { this.repository = repository; }
  public List<PosSaleCategory> findByHeadquarterId(long id, boolean includeInactive) {
    var rows = includeInactive ? repository.findByHeadquarterIdAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(id) : repository.findByHeadquarterIdAndDeletedAtIsNullAndActiveTrueOrderByDisplayOrderAscNameAsc(id);
    return rows.stream().map(PosSaleCategoryPersistenceMapper::toDomain).toList();
  }
  public Optional<PosSaleCategory> findById(long hq, long id) { return repository.findByIdAndHeadquarterIdAndDeletedAtIsNull(id, hq).map(PosSaleCategoryPersistenceMapper::toDomain); }
  public Optional<PosSaleCategory> findActiveByName(long hq, String name) { return repository.findActiveByName(hq, name.strip()).map(PosSaleCategoryPersistenceMapper::toDomain); }
  public long countActiveCatalogItems(long hq, long category) { return repository.countActiveCatalogItems(hq, category); }
  public PosSaleCategory save(PosSaleCategory category) { return PosSaleCategoryPersistenceMapper.toDomain(repository.save(PosSaleCategoryPersistenceMapper.toEntity(category))); }
}
