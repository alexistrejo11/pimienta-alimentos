package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OrganizationJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrganizationJpaRepository
    extends JpaRepository<OrganizationJpaEntity, Long>,
        JpaSpecificationExecutor<OrganizationJpaEntity> {

  Optional<OrganizationJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
