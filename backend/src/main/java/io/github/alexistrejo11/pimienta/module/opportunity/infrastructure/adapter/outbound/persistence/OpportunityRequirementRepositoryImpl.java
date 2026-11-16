package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.RequirementNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRequirementRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityRequirementJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityRequirementRepositoryImpl implements OpportunityRequirementRepository {

  private final OpportunityRequirementJpaRepository jpaRepository;

  public OpportunityRequirementRepositoryImpl(OpportunityRequirementJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<OpportunityRequirement> findById(long id) {
    return jpaRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(OpportunityRequirementPersistenceMapper::toDomain);
  }

  @Override
  public List<OpportunityRequirement> findByOpportunityId(long opportunityId) {
    return jpaRepository
        .findByOpportunityIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(opportunityId)
        .stream()
        .map(OpportunityRequirementPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public OpportunityRequirement save(OpportunityRequirement requirement) {
    OpportunityRequirementJpaEntity entity;
    if (requirement.getId() != null && requirement.getId() > 0) {
      entity =
          jpaRepository
              .findById(requirement.getId())
              .orElseThrow(() -> new RequirementNotFoundException(requirement.getId()));
    } else {
      entity = new OpportunityRequirementJpaEntity();
    }
    OpportunityRequirementPersistenceMapper.apply(requirement, entity);
    return OpportunityRequirementPersistenceMapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public long countMandatoryNotSatisfied(long opportunityId) {
    return jpaRepository.countMandatoryNotSatisfied(opportunityId);
  }
}
