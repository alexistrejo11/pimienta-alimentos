package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.entity.PosHealthSnapshotJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosHealthSnapshotSpringDataRepository
    extends JpaRepository<PosHealthSnapshotJpaEntity, UUID> {

  List<PosHealthSnapshotJpaEntity> findByDeviceIdOrderByReceivedAtDescIdDesc(UUID deviceId, Pageable pageable);

  long countByDeviceId(UUID deviceId);

  @Query("""
      SELECT s FROM PosHealthSnapshotJpaEntity s
      WHERE (:headquarterId IS NULL OR s.headquarterId = :headquarterId)
        AND (:cursorAt IS NULL OR s.receivedAt < :cursorAt
             OR (s.receivedAt = :cursorAt AND s.id < :cursorId))
      ORDER BY s.receivedAt DESC, s.id DESC
      """)
  List<PosHealthSnapshotJpaEntity> findActivity(
      @Param("headquarterId") Long headquarterId,
      @Param("cursorAt") Instant cursorAt,
      @Param("cursorId") UUID cursorId,
      Pageable pageable);

  @Query(value = """
      SELECT * FROM (
        SELECT s.*, ROW_NUMBER() OVER (PARTITION BY s.device_id ORDER BY s.received_at DESC, s.id DESC) AS rn
        FROM pos_health_snapshots s
        WHERE (:headquarterId IS NULL OR s.headquarter_id = :headquarterId)
      ) latest WHERE latest.rn = 1
      """, nativeQuery = true)
  List<LatestHealthProjection> findLatest(@Param("headquarterId") Long headquarterId);

  interface LatestHealthProjection {
    UUID getId();
    UUID getDeviceId();
    Long getHeadquarterId();
    String getSyncState();
    int getPendingEvents();
    long getOldestPendingAgeSeconds();
    String getAppVersion();
    Instant getReceivedAt();
  }
}
