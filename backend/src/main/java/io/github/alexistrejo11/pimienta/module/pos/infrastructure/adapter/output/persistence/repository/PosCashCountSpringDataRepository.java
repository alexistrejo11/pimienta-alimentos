package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosCashCountJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosCashCountSpringDataRepository extends JpaRepository<PosCashCountJpaEntity, UUID> {
  List<PosCashCountJpaEntity> findByShiftIdOrderBySubmittedAtAsc(UUID shiftId);
}
