package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosSyncEventPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosSyncEventSpringDataRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosSyncEventRepositoryImpl implements PosSyncEventRepository {

  private final PosSyncEventSpringDataRepository jpa;

  public PosSyncEventRepositoryImpl(PosSyncEventSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<PosSyncEvent> findByEventId(UUID eventId) {
    return jpa.findByEventIdAndDeletedAtIsNull(eventId)
        .map(PosSyncEventPersistenceMapper::toDomain);
  }

  @Override
  public Optional<PosSyncEvent> findByDeviceIdAndDeviceSequence(UUID deviceId, long deviceSequence) {
    return jpa.findByDeviceIdAndDeviceSequenceAndDeletedAtIsNull(deviceId, deviceSequence)
        .map(PosSyncEventPersistenceMapper::toDomain);
  }

  @Override
  public PosSyncEvent save(PosSyncEvent event) {
    boolean exists = jpa.existsById(event.getId());
    var entity = PosSyncEventPersistenceMapper.toEntity(event);
    entity.setNewEntity(!exists);
    return PosSyncEventPersistenceMapper.toDomain(jpa.save(entity));
  }

  @Override
  public Page<PosSyncEvent> findAcceptedByEventTypes(
      PosReportFilterQuery filter, Collection<String> eventTypes, Pageable pageable) {
    return jpa.findAcceptedByEventTypes(
            filter.headquarterId(),
            filter.from(),
            filter.to(),
            filter.shiftId(),
            filter.eventType(),
            eventTypes,
            PosEventResultStatus.ACCEPTED,
            pageable)
        .map(PosSyncEventPersistenceMapper::toDomain);
  }

  @Override
  public long countAcceptedByEventType(
      long headquarterId, Instant from, Instant to, String eventType) {
    return jpa.countAcceptedByEventType(
        headquarterId, from, to, eventType, PosEventResultStatus.ACCEPTED);
  }

  @Override
  public Instant findLastAcceptedEventAt(
      long headquarterId, Instant from, Instant to, String eventType) {
    return jpa.findLastAcceptedEventAt(
        headquarterId, from, to, eventType, PosEventResultStatus.ACCEPTED);
  }

  @Override
  public Map<UUID, PosEventResultStatus> findSyncStatusesByEventIds(Collection<UUID> eventIds) {
    if (eventIds == null || eventIds.isEmpty()) {
      return Map.of();
    }
    return jpa.findByEventIds(eventIds).stream()
        .collect(
            Collectors.toMap(
                io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output
                    .persistence.entity.PosSyncEventJpaEntity::getEventId,
                e -> e.getStatus(),
                (a, b) -> a));
  }
}
