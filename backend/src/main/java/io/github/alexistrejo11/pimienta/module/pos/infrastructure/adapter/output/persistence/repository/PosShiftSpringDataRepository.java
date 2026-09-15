package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosShiftJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosShiftSpringDataRepository extends JpaRepository<PosShiftJpaEntity, UUID> {
  Page<PosShiftJpaEntity> findByHeadquarterIdOrderByOpenedAtDesc(long headquarterId, Pageable pageable);
  Page<PosShiftJpaEntity> findByHeadquarterIdInOrderByOpenedAtDesc(List<Long> headquarterIds, Pageable pageable);
  Page<PosShiftJpaEntity> findAllByOrderByOpenedAtDesc(Pageable pageable);
  Optional<PosShiftJpaEntity> findByShiftIdAndHeadquarterId(UUID shiftId, long headquarterId);

  @Query(
      """
      SELECT s FROM PosShiftJpaEntity s
      WHERE s.headquarterId IN :headquarterIds
        AND (:status IS NULL OR s.status = :status)
        AND (:cashierOperatorId IS NULL OR s.cashierOperatorId = :cashierOperatorId)
        AND (
          :useClosedDateFilter = false OR (
            s.closedAt IS NOT NULL
            AND (:closedFrom IS NULL OR s.closedAt >= :closedFrom)
            AND (:closedTo IS NULL OR s.closedAt < :closedTo)
          )
        )
        AND (
          :useOpenedDateFilter = false OR (
            (:openedFrom IS NULL OR s.openedAt >= :openedFrom)
            AND (:openedTo IS NULL OR s.openedAt < :openedTo)
          )
        )
      ORDER BY s.openedAt DESC
      """)
  Page<PosShiftJpaEntity> findFilteredByHeadquarterIds(
      @Param("headquarterIds") List<Long> headquarterIds,
      @Param("closedFrom") Instant closedFrom,
      @Param("closedTo") Instant closedTo,
      @Param("openedFrom") Instant openedFrom,
      @Param("openedTo") Instant openedTo,
      @Param("useClosedDateFilter") boolean useClosedDateFilter,
      @Param("useOpenedDateFilter") boolean useOpenedDateFilter,
      @Param("status") String status,
      @Param("cashierOperatorId") Long cashierOperatorId,
      Pageable pageable);

  @Query(
      """
      SELECT s FROM PosShiftJpaEntity s
      WHERE (:status IS NULL OR s.status = :status)
        AND (:cashierOperatorId IS NULL OR s.cashierOperatorId = :cashierOperatorId)
        AND (
          :useClosedDateFilter = false OR (
            s.closedAt IS NOT NULL
            AND (:closedFrom IS NULL OR s.closedAt >= :closedFrom)
            AND (:closedTo IS NULL OR s.closedAt < :closedTo)
          )
        )
        AND (
          :useOpenedDateFilter = false OR (
            (:openedFrom IS NULL OR s.openedAt >= :openedFrom)
            AND (:openedTo IS NULL OR s.openedAt < :openedTo)
          )
        )
      ORDER BY s.openedAt DESC
      """)
  Page<PosShiftJpaEntity> findAllFiltered(
      @Param("closedFrom") Instant closedFrom,
      @Param("closedTo") Instant closedTo,
      @Param("openedFrom") Instant openedFrom,
      @Param("openedTo") Instant openedTo,
      @Param("useClosedDateFilter") boolean useClosedDateFilter,
      @Param("useOpenedDateFilter") boolean useOpenedDateFilter,
      @Param("status") String status,
      @Param("cashierOperatorId") Long cashierOperatorId,
      Pageable pageable);
}
