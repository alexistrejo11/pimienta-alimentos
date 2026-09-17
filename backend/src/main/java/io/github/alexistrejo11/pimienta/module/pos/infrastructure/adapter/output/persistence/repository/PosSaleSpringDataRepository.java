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
            AND (:lineType IS NULL OR EXISTS (
                  SELECT 1 FROM PosSaleLineJpaEntity l
                  WHERE l.sale = s AND l.lineType = :lineType AND l.deletedAt IS NULL
                ))
            AND (:openProductsOnly = false OR EXISTS (
                  SELECT 1 FROM PosSaleLineJpaEntity l
                  WHERE l.sale = s AND l.lineType = 'OPEN_AMOUNT' AND l.deletedAt IS NULL
                ))
            AND EXISTS (
                  SELECT 1 FROM PosSyncEventJpaEntity e
                  WHERE e.eventId = s.eventId
                    AND e.deletedAt IS NULL
                    AND e.status IN :syncStatuses
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
                    AND e.status IN :syncStatuses
                )
          """)
  Page<PosSaleJpaEntity> findAcceptedSales(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("shiftId") UUID shiftId,
      @Param("productId") Long productId,
      @Param("lineType") io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType lineType,
      @Param("openProductsOnly") boolean openProductsOnly,
      @Param("syncStatuses") java.util.Collection<PosEventResultStatus> syncStatuses,
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
            AND (:lineType IS NULL OR l.lineType = :lineType)
            AND (:openProductsOnly = false OR l.lineType = 'OPEN_AMOUNT')
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
      @Param("lineType") io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType lineType,
      @Param("openProductsOnly") boolean openProductsOnly,
      @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT COALESCE(SUM(s.totalCentavos), 0), COUNT(s)
      FROM PosSaleJpaEntity s
      WHERE s.deletedAt IS NULL
        AND s.headquarterId = :headquarterId
        AND s.occurredAt >= :from
        AND s.occurredAt < :to
        AND EXISTS (
              SELECT 1 FROM PosSyncEventJpaEntity e
              WHERE e.eventId = s.eventId
                AND e.deletedAt IS NULL
                AND e.status = :accepted
            )
      """)
  /** Prefer {@code List} — Spring Data treats a bare {@code Object[]} as “array of rows”. */
  List<Object[]> summarizeAcceptedSales(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT
        COALESCE(SUM(CASE WHEN e.status = :accepted THEN l.subtotalCentavos ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN e.status = :accepted THEN 1 ELSE 0 END), 0),
        COUNT(DISTINCT CASE WHEN e.status = :accepted THEN s.saleId ELSE NULL END),
        COALESCE(SUM(CASE WHEN e.status = 'REQUIRES_REVIEW' THEN 1 ELSE 0 END), 0)
      FROM PosSaleLineJpaEntity l
      JOIN l.sale s
      JOIN PosSyncEventJpaEntity e ON e.eventId = s.eventId
      WHERE s.deletedAt IS NULL AND l.deletedAt IS NULL
        AND l.lineType = 'OPEN_AMOUNT'
        AND s.headquarterId = :headquarterId
        AND s.occurredAt >= :from AND s.occurredAt < :to
        AND e.deletedAt IS NULL
      """)
  List<Object[]> summarizeOpenProducts(
      @Param("headquarterId") long headquarterId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT COUNT(DISTINCT s.saleId)
      FROM PosSaleJpaEntity s
      WHERE s.deletedAt IS NULL
        AND s.shiftId = :shiftId
        AND s.status = io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus.CONFIRMED
        AND EXISTS (
              SELECT 1 FROM PosSyncEventJpaEntity e
              WHERE e.eventId = s.eventId
                AND e.deletedAt IS NULL
                AND e.status = :accepted
            )
      """)
  long countAcceptedConfirmedSalesForShift(
      @Param("shiftId") UUID shiftId, @Param("accepted") PosEventResultStatus accepted);

  @Query(
      """
      SELECT p.method, COALESCE(SUM(p.amountCentavos), 0)
      FROM PosSalePaymentJpaEntity p
      JOIN p.sale s
      WHERE s.deletedAt IS NULL
        AND s.shiftId = :shiftId
        AND s.status = io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus.CONFIRMED
        AND EXISTS (
              SELECT 1 FROM PosSyncEventJpaEntity e
              WHERE e.eventId = s.eventId
                AND e.deletedAt IS NULL
                AND e.status = :accepted
            )
      GROUP BY p.method
      """)
  List<Object[]> sumPaymentsByMethodForShift(
      @Param("shiftId") UUID shiftId, @Param("accepted") PosEventResultStatus accepted);
}
