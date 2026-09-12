package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.PosSaleCategoryJpaEntity;
import java.time.LocalDateTime;

public final class PosSaleCategoryPersistenceMapper {
  private PosSaleCategoryPersistenceMapper() {}

  public static PosSaleCategory toDomain(PosSaleCategoryJpaEntity e) {
    PosSaleCategory value = PosSaleCategory.create(e.getHeadquarterId(), e.getName(), e.getDisplayOrder());
    value.setId(e.getId());
    value.setCreatedAt(e.getCreatedAt());
    value.setUpdatedAt(e.getUpdatedAt());
    value.setDeletedAt(e.getDeletedAt());
    value.setVersion(e.getVersion());
    if (!e.isActive()) value.archive();
    return value;
  }

  public static PosSaleCategoryJpaEntity toEntity(PosSaleCategory d) {
    PosSaleCategoryJpaEntity e = new PosSaleCategoryJpaEntity();
    if (d.getId() != null && d.getId() > 0) e.setId(d.getId());
    e.setHeadquarterId(d.getHeadquarterId()); e.setName(d.getName());
    e.setDisplayOrder(d.getDisplayOrder()); e.setActive(d.isActive());
    e.setCreatedAt(d.getCreatedAt() != null ? d.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(d.getDeletedAt()); e.setVersion(d.getVersion() != null ? d.getVersion() : 0L);
    return e;
  }
}
