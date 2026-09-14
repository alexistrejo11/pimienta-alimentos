package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosCashMovementJpaEntity;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosCashMovementSpringDataRepository extends JpaRepository<PosCashMovementJpaEntity, UUID> { List<PosCashMovementJpaEntity> findByShiftIdOrderByOccurredAtAsc(UUID shiftId); }
