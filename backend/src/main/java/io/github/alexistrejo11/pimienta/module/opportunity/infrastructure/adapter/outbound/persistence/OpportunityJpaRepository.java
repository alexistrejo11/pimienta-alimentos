package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityJpaRepository
    extends JpaRepository<OpportunityJpaEntity, Long>,
        JpaSpecificationExecutor<OpportunityJpaEntity> {

  Optional<OpportunityJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  @Query(
      """
      select case when count(o) > 0 then true else false end
      from OpportunityJpaEntity o
      where o.organizationId = :organizationId and o.deletedAt is null
      """)
  boolean existsActiveByOrganizationId(@Param("organizationId") long organizationId);
}
