package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.outbound.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.outbound.persistence.entity.PosSyncIncidentJpaEntity;
import java.time.LocalDateTime;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

public final class PosSyncIncidentPersistenceMapper {

  private PosSyncIncidentPersistenceMapper() {}

  public static PosSyncIncident toDomain(PosSyncIncidentJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosSyncIncident.builder()
        .withId(entity.getId())
        .withEventId(entity.getEventId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withReasonCode(entity.getReasonCode())
        .withDetail(entity.getDetail())
        .withAcceptedAt(entity.getAcceptedAt())
        .withAcceptedBy(entity.getAcceptedBy())
        .withAcceptLabel(entity.getAcceptLabel())
        .withAcceptNote(entity.getAcceptNote())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosSyncIncidentJpaEntity toEntity(PosSyncIncident domain) {
    PosSyncIncidentJpaEntity e = new PosSyncIncidentJpaEntity();
    e.setId(domain.getId());
    e.setEventId(domain.getEventId());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setReasonCode(domain.getReasonCode());
    e.setDetail(Strings.blankToNull(domain.getDetail()));
    e.setAcceptedAt(domain.getAcceptedAt());
    e.setAcceptedBy(domain.getAcceptedBy());
    e.setAcceptLabel(Strings.blankToNull(domain.getAcceptLabel()));
    e.setAcceptNote(Strings.blankToNull(domain.getAcceptNote()));
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

}
