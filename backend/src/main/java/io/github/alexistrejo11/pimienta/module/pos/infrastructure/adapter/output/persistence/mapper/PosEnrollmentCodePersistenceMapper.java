package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosEnrollmentCodeJpaEntity;
import java.time.LocalDateTime;

public final class PosEnrollmentCodePersistenceMapper {

  private PosEnrollmentCodePersistenceMapper() {}

  public static PosEnrollmentCode toDomain(PosEnrollmentCodeJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosEnrollmentCode.builder()
        .withId(entity.getId())
        .withCode(entity.getCode())
        .withHeadquarterId(entity.getHeadquarterId())
        .withExpiresAt(entity.getExpiresAt())
        .withConsumedAt(entity.getConsumedAt())
        .withConsumedByDeviceId(entity.getConsumedByDeviceId())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosEnrollmentCodeJpaEntity toEntity(PosEnrollmentCode domain) {
    PosEnrollmentCodeJpaEntity e = new PosEnrollmentCodeJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setCode(domain.getCode());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setExpiresAt(domain.getExpiresAt());
    e.setConsumedAt(domain.getConsumedAt());
    e.setConsumedByDeviceId(domain.getConsumedByDeviceId());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }
}
