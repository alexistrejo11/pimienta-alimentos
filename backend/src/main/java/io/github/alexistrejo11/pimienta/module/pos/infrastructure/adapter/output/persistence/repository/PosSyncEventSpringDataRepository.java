package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSyncEventJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSyncEventSpringDataRepository
    extends JpaRepository<PosSyncEventJpaEntity, UUID> {

  Optional<PosSyncEventJpaEntity> findByEventIdAndDeletedAtIsNull(UUID eventId);

  Optional<PosSyncEventJpaEntity> findByDeviceIdAndDeviceSequenceAndDeletedAtIsNull(
      UUID deviceId, long deviceSequence);

  boolean existsByEventIdAndDeletedAtIsNull(UUID eventId);

  @Query(
      value =
          """
          SELECT e FROM PosSyncEventJpaEntity e
          WHERE e.deletedAt IS NULL
            AND e.status = :accepted
            AND e.headquarterId = :headquarterId
            AND e.occurredAt >= :from
            AND e.occurredAt < :to
            AND e.eventType IN :eventTypes
            AND (:shiftId IS NULL OR e.shiftId = :shiftId)
            AND (:eventType IS NULL OR e.eventType = :eventType)
          """,
      countQuery =
          """
          SELECT COUNT(e) FROM PosSyncEventJpaEntity e
          WHERE e.deletedAt IS NULL
            AND e.status = :accepted
            AND e.headquarterId = :headquarterId
            AND e.occurredAt >= :from
            AND e.occurredAt < :to
            AND e.eventType IN :eventTypes
            AND (:shiftId IS NULL OR e.shiftId = :shiftId)
            AND (:eventType IS NULL OR e.eventType = :eventType)
          """)
  Page<PosSyncEventJpaEntity> findAcceptedByEventTypes(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("shiftId") UUID shiftId,
      @Param("eventType") String eventType,
      @Param("eventTypes") Collection<String> eventTypes,
      @Param("accepted") PosEventResultStatus accepted,
      Pageable pageable);

  @Query(
      """
      SELECT COUNT(e) FROM PosSyncEventJpaEntity e
      WHERE e.deletedAt IS NULL
        AND e.status = :accepted
        AND e.headquarterId = :headquarterId
        AND e.occurredAt >= :from
        AND e.occurredAt < :to
        AND e.eventType = :eventType
      """)
  long countAcceptedByEventType(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("eventType") String eventType,
      @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT MAX(e.occurredAt) FROM PosSyncEventJpaEntity e
      WHERE e.deletedAt IS NULL
        AND e.status = :accepted
        AND e.headquarterId = :headquarterId
        AND e.occurredAt >= :from
        AND e.occurredAt < :to
        AND e.eventType = :eventType
      """)
  Instant findLastAcceptedEventAt(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("eventType") String eventType,
      @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT e FROM PosSyncEventJpaEntity e
      WHERE e.deletedAt IS NULL AND e.eventId IN :eventIds
      """)
  List<PosSyncEventJpaEntity> findByEventIds(@Param("eventIds") Collection<UUID> eventIds);
}
