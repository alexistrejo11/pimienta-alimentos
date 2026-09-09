package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosDeviceJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosDeviceSpringDataRepository extends JpaRepository<PosDeviceJpaEntity, UUID> {

  Optional<PosDeviceJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

  Page<PosDeviceJpaEntity> findByDeletedAtIsNull(Pageable pageable);

  Page<PosDeviceJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(
      Long headquarterId, Pageable pageable);

  long countByHeadquarterIdAndStatusNotAndDeletedAtIsNull(
      Long headquarterId, PosDeviceStatus status);
}
