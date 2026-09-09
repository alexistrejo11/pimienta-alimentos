package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosOperatorJpaEntity;
import java.time.LocalDateTime;
import java.util.HashSet;

public final class PosOperatorPersistenceMapper {

  private PosOperatorPersistenceMapper() {}

  public static PosOperator toDomain(PosOperatorJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosOperator.builder()
        .withId(entity.getId())
        .withUserId(entity.getUserId())
        .withDisplayName(entity.getDisplayName())
        .withPosRole(entity.getPosRole())
        .withPinHash(entity.getPinHash())
        .withActive(entity.isActive())
        .withHeadquarterIds(
            entity.getHeadquarterIds() != null
                ? new HashSet<>(entity.getHeadquarterIds())
                : new HashSet<>())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosOperatorJpaEntity toEntity(PosOperator domain) {
    PosOperatorJpaEntity e = new PosOperatorJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setUserId(domain.getUserId());
    e.setDisplayName(
        domain.getDisplayName() != null && !domain.getDisplayName().isBlank()
            ? domain.getDisplayName().strip()
            : "Operator");
    e.setPosRole(domain.getPosRole() != null ? domain.getPosRole() : PosRole.CASHIER);
    e.setPinHash(domain.getPinHash());
    e.setActive(domain.isActive());
    e.setHeadquarterIds(new HashSet<>(domain.getHeadquarterIds()));
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }
}
