package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityJpaEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityRepositoryImpl implements OpportunityRepository {

  private final OpportunityJpaRepository jpaRepository;

  public OpportunityRepositoryImpl(OpportunityJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Opportunity> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(OpportunityPersistenceMapper::toDomain);
  }

  @Override
  public Page<Opportunity> search(OpportunitySearchCriteria criteria, Pageable pageable) {
    return jpaRepository
        .findAll(OpportunitySpecifications.fromCriteria(criteria), pageable)
        .map(OpportunityPersistenceMapper::toDomain);
  }

  @Override
  public Opportunity save(Opportunity opportunity) {
    OpportunityJpaEntity entity;
    if (opportunity.getId() != null && opportunity.getId() > 0) {
      entity =
          jpaRepository
              .findById(opportunity.getId())
              .orElseThrow(() -> new OpportunityNotFoundException(opportunity.getId()));
    } else {
      entity = new OpportunityJpaEntity();
    }
    OpportunityPersistenceMapper.apply(opportunity, entity);
    return OpportunityPersistenceMapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public boolean existsByOrganizationId(long organizationId) {
    return jpaRepository.existsActiveByOrganizationId(organizationId);
  }
}
