package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSyncEventJpaEntity;
import java.time.Instant;
import java.time.LocalDateTime;

public final class PosSyncEventPersistenceMapper {

  private PosSyncEventPersistenceMapper() {}

  public static PosSyncEvent toDomain(PosSyncEventJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosSyncEvent.builder()
        .withId(entity.getEventId())
        .withEventType(entity.getEventType())
        .withSchemaVersion(entity.getSchemaVersion())
        .withDeviceId(entity.getDeviceId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withDeviceSequence(entity.getDeviceSequence())
        .withAggregateId(entity.getAggregateId())
        .withShiftId(entity.getShiftId())
        .withOccurredAt(entity.getOccurredAt())
        .withPayloadJson(entity.getPayload())
        .withStatus(entity.getStatus())
        .withServerReceivedAt(entity.getServerReceivedAt())
        .withIncidentId(entity.getIncidentId())
        .withMessage(entity.getMessage())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosSyncEventJpaEntity toEntity(PosSyncEvent domain) {
    PosSyncEventJpaEntity e = new PosSyncEventJpaEntity();
    e.setEventId(domain.getId());
    e.setEventType(domain.getEventType());
    e.setSchemaVersion(domain.getSchemaVersion());
    e.setDeviceId(domain.getDeviceId());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setDeviceSequence(domain.getDeviceSequence());
    e.setAggregateId(domain.getAggregateId());
    e.setShiftId(domain.getShiftId());
    e.setOccurredAt(domain.getOccurredAt() != null ? domain.getOccurredAt() : Instant.now());
    e.setPayload(
        domain.getPayloadJson() != null && !domain.getPayloadJson().isBlank()
            ? domain.getPayloadJson()
            : "{}");
    e.setStatus(
        domain.getStatus() != null ? domain.getStatus() : PosEventResultStatus.ACCEPTED);
    e.setServerReceivedAt(
        domain.getServerReceivedAt() != null ? domain.getServerReceivedAt() : Instant.now());
    e.setIncidentId(domain.getIncidentId());
    e.setMessage(blankToNull(domain.getMessage()));
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
