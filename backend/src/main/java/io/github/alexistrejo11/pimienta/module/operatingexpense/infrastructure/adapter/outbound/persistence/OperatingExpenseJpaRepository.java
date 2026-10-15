package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence.entity.OperatingExpenseJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperatingExpenseJpaRepository
    extends JpaRepository<OperatingExpenseJpaEntity, Long>, JpaSpecificationExecutor<OperatingExpenseJpaEntity> {

  Optional<OperatingExpenseJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
