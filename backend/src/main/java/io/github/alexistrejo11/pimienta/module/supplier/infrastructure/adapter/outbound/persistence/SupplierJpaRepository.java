package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierJpaRepository
    extends JpaRepository<SupplierJpaEntity, Long>, JpaSpecificationExecutor<SupplierJpaEntity> {

  Optional<SupplierJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
