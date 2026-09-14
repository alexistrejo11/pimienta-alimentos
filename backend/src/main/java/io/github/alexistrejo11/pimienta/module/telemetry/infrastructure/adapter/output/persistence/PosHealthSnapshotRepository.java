package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.entity.PosHealthSnapshotJpaEntity;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.repository.PosHealthSnapshotSpringDataRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosHealthSnapshotRepository {
  private final PosHealthSnapshotSpringDataRepository jpa;

  public PosHealthSnapshotRepository(PosHealthSnapshotSpringDataRepository jpa) { this.jpa = jpa; }

  public PosHealthSnapshotJpaEntity save(UUID deviceId, long headquarterId, String state,
      int pending, long oldestAge, String appVersion) {
    var entity = new PosHealthSnapshotJpaEntity();
    entity.setId(UUID.randomUUID());
    entity.setDeviceId(deviceId);
    entity.setHeadquarterId(headquarterId);
    entity.setSyncState(state);
    entity.setPendingEvents(pending);
    entity.setOldestPendingAgeSeconds(oldestAge);
    entity.setAppVersion(appVersion);
    entity.setReceivedAt(Instant.now());
    return jpa.save(entity);
  }

  public List<PosHealthSnapshotJpaEntity> findByDeviceId(UUID id, Pageable pageable) {
    return jpa.findByDeviceIdOrderByReceivedAtDescIdDesc(id, pageable);
  }

  public long countByDeviceId(UUID id) {
    return jpa.countByDeviceId(id);
  }

  public List<PosHealthSnapshotJpaEntity> findActivity(Long hq, Instant at, UUID id, Pageable pageable) {
    return jpa.findActivity(hq, at, id, pageable);
  }

  public List<PosHealthSnapshotSpringDataRepository.LatestHealthProjection> findLatest(Long hq) {
    return jpa.findLatest(hq);
  }
}
