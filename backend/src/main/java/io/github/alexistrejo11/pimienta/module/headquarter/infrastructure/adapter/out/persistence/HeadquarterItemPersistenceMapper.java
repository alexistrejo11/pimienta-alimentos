package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterItemJpaEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class HeadquarterItemPersistenceMapper {

  private HeadquarterItemPersistenceMapper() {}

  public static HeadquarterItem toDomain(HeadquarterItemJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return HeadquarterItem.builder()
        .withId(entity.getId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withItemId(entity.getItemId())
        .withSaleCategory(entity.getSaleCategory())
        .withSalePrice(entity.getSalePrice())
        .withAvailable(entity.isAvailable())
        .withStockPolicy(entity.getStockPolicy())
        .withNegativeStockLimit(entity.getNegativeStockLimit())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static HeadquarterItemJpaEntity toEntity(HeadquarterItem domain) {
    HeadquarterItemJpaEntity e = new HeadquarterItemJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setItemId(domain.getItemId());
    e.setSaleCategory(
        domain.getSaleCategory() != null && !domain.getSaleCategory().isBlank()
            ? domain.getSaleCategory().strip()
            : "GENERAL");
    e.setSalePrice(domain.getSalePrice() != null ? domain.getSalePrice() : BigDecimal.ZERO);
    e.setAvailable(domain.isAvailable());
    e.setStockPolicy(domain.getStockPolicy());
    e.setNegativeStockLimit(domain.getNegativeStockLimit());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }
}
