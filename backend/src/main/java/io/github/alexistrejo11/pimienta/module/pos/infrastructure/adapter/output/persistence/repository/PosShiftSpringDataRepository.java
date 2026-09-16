package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosShiftJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosShiftSpringDataRepository extends JpaRepository<PosShiftJpaEntity, UUID> {
  Page<PosShiftJpaEntity> findByHeadquarterIdOrderByOpenedAtDesc(long headquarterId, Pageable pageable);

  Page<PosShiftJpaEntity> findByHeadquarterIdInOrderByOpenedAtDesc(
      List<Long> headquarterIds, Pageable pageable);

  Page<PosShiftJpaEntity> findAllByOrderByOpenedAtDesc(Pageable pageable);

  Optional<PosShiftJpaEntity> findByShiftIdAndHeadquarterId(UUID shiftId, long headquarterId);
}
