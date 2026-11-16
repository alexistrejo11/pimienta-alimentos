package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.ProposalNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityProposalRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityProposalJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityProposalRepositoryImpl implements OpportunityProposalRepository {

  private final OpportunityProposalJpaRepository jpaRepository;

  public OpportunityProposalRepositoryImpl(OpportunityProposalJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<OpportunityProposal> findById(long id) {
    return jpaRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(OpportunityProposalPersistenceMapper::toDomain);
  }

  @Override
  public List<OpportunityProposal> findByOpportunityId(long opportunityId) {
    return jpaRepository
        .findByOpportunityIdAndDeletedAtIsNullOrderByIdAsc(opportunityId)
        .stream()
        .map(OpportunityProposalPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public OpportunityProposal save(OpportunityProposal proposal) {
    OpportunityProposalJpaEntity entity;
    if (proposal.getId() != null && proposal.getId() > 0) {
      entity =
          jpaRepository
              .findById(proposal.getId())
              .orElseThrow(() -> new ProposalNotFoundException(proposal.getId()));
    } else {
      entity = new OpportunityProposalJpaEntity();
    }
    OpportunityProposalPersistenceMapper.apply(proposal, entity);
    return OpportunityProposalPersistenceMapper.toDomain(jpaRepository.save(entity));
  }
}
