package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSyncTombstoneJpaEntity;

public final class PosSyncTombstonePersistenceMapper {

  private PosSyncTombstonePersistenceMapper() {}

  public static PosSyncTombstone toDomain(PosSyncTombstoneJpaEntity entity) {
    return new PosSyncTombstone(
        entity.getId(),
        entity.getHeadquarterId(),
        entity.getEntity(),
        entity.getEntityId(),
        entity.getCreatedAt());
  }

  public static PosSyncTombstoneJpaEntity toEntity(PosSyncTombstone tombstone) {
    PosSyncTombstoneJpaEntity entity = new PosSyncTombstoneJpaEntity();
    entity.setId(tombstone.id());
    entity.setHeadquarterId(tombstone.headquarterId());
    entity.setEntity(tombstone.entity());
    entity.setEntityId(tombstone.entityId());
    entity.setCreatedAt(tombstone.createdAt());
    return entity;
  }
}
