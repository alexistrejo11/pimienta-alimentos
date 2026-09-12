package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterPosSettingsJpaEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PosOperationalConfigPersistenceMapper {

  private PosOperationalConfigPersistenceMapper() {}

  public static PosOperationalConfig toDomain(HeadquarterPosSettingsJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosOperationalConfig.builder()
        .withId(entity.getId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withCurrency(entity.getCurrency())
        .withCatalogStaleWarnHours(entity.getCatalogStaleWarnHours())
        .withCatalogStaleBlockHours(entity.getCatalogStaleBlockHours())
        .withOpenAmountCategories(
            entity.getOpenAmountCategories() != null
                ? new ArrayList<>(entity.getOpenAmountCategories())
                : List.of())
        .withDefaultNegativeStockLimit(entity.getDefaultNegativeStockLimit())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static HeadquarterPosSettingsJpaEntity toEntity(PosOperationalConfig domain) {
    HeadquarterPosSettingsJpaEntity e = new HeadquarterPosSettingsJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setCurrency(domain.getCurrency());
    e.setCatalogStaleWarnHours(domain.getCatalogStaleWarnHours());
    e.setCatalogStaleBlockHours(domain.getCatalogStaleBlockHours());
    e.setOpenAmountCategories(new ArrayList<>(domain.getOpenAmountCategories()));
    e.setDefaultNegativeStockLimit(domain.getDefaultNegativeStockLimit());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }
}
