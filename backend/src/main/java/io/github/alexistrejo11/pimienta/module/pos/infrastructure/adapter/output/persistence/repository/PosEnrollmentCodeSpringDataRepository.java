package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosEnrollmentCodeJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosEnrollmentCodeSpringDataRepository
    extends JpaRepository<PosEnrollmentCodeJpaEntity, Long> {

  Optional<PosEnrollmentCodeJpaEntity> findByCodeAndDeletedAtIsNull(String code);
}
