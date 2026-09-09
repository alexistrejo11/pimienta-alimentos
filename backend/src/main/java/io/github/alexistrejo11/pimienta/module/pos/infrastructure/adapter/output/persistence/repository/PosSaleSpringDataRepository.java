package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSaleJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSaleSpringDataRepository extends JpaRepository<PosSaleJpaEntity, UUID> {

  Optional<PosSaleJpaEntity> findBySaleIdAndDeletedAtIsNull(UUID saleId);

  boolean existsBySaleIdAndDeletedAtIsNull(UUID saleId);

  @Query(
      value =
          """
          SELECT s FROM PosSaleJpaEntity s
          WHERE s.deletedAt IS NULL
            AND s.headquarterId = :headquarterId
            AND s.occurredAt >= :from
            AND s.occurredAt < :to
            AND (:shiftId IS NULL OR s.shiftId = :shiftId)
            AND (:productId IS NULL OR EXISTS (
                  SELECT 1 FROM PosSaleLineJpaEntity l
                  WHERE l.sale = s AND l.productId = :productId AND l.deletedAt IS NULL
                ))
            AND EXISTS (
                  SELECT 1 FROM PosSyncEventJpaEntity e
                  WHERE e.eventId = s.eventId
                    AND e.deletedAt IS NULL
                    AND e.status = :accepted
                )
          """,
      countQuery =
          """
          SELECT COUNT(s) FROM PosSaleJpaEntity s
          WHERE s.deletedAt IS NULL
            AND s.headquarterId = :headquarterId
            AND s.occurredAt >= :from
            AND s.occurredAt < :to
            AND (:shiftId IS NULL OR s.shiftId = :shiftId)
            AND (:productId IS NULL OR EXISTS (
                  SELECT 1 FROM PosSaleLineJpaEntity l
                  WHERE l.sale = s AND l.productId = :productId AND l.deletedAt IS NULL
                ))
            AND EXISTS (
                  SELECT 1 FROM PosSyncEventJpaEntity e
                  WHERE e.eventId = s.eventId
                    AND e.deletedAt IS NULL
                    AND e.status = :accepted
                )
          """)
  Page<PosSaleJpaEntity> findAcceptedSales(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("shiftId") UUID shiftId,
      @Param("productId") Long productId,
      @Param("accepted") PosEventResultStatus accepted,
      Pageable pageable);

  @Query(
      """
      SELECT new io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow(
        l.productId,
        l.productName,
        COALESCE(SUM(l.quantity), 0),
        COALESCE(SUM(l.subtotalCentavos), 0),
        COUNT(DISTINCT s.saleId)
      )
      FROM PosSaleLineJpaEntity l
      JOIN l.sale s
      WHERE s.deletedAt IS NULL
        AND l.deletedAt IS NULL
        AND s.headquarterId = :headquarterId
        AND s.occurredAt >= :from
        AND s.occurredAt < :to
        AND (:shiftId IS NULL OR s.shiftId = :shiftId)
        AND (:productId IS NULL OR l.productId = :productId)
        AND EXISTS (
              SELECT 1 FROM PosSyncEventJpaEntity e
              WHERE e.eventId = s.eventId
                AND e.deletedAt IS NULL
                AND e.status = :accepted
            )
      GROUP BY l.productId, l.productName
      ORDER BY l.productName ASC
      """)
  List<PosProductReportRow> findAcceptedProductTotals(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("shiftId") UUID shiftId,
      @Param("productId") Long productId,
      @Param("accepted") PosEventResultStatus accepted);
}
