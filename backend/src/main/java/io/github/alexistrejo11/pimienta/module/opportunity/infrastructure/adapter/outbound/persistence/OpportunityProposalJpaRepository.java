package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityProposalJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityProposalJpaRepository
    extends JpaRepository<OpportunityProposalJpaEntity, Long> {

  Optional<OpportunityProposalJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  List<OpportunityProposalJpaEntity> findByOpportunityIdAndDeletedAtIsNullOrderByIdAsc(
      Long opportunityId);
}
