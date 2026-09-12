package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSyncTombstoneJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosSyncTombstoneSpringDataRepository
    extends JpaRepository<PosSyncTombstoneJpaEntity, Long> {

  List<PosSyncTombstoneJpaEntity> findByHeadquarterIdAndCreatedAtAfterOrderByCreatedAtAsc(
      Long headquarterId, LocalDateTime createdAt);
}
