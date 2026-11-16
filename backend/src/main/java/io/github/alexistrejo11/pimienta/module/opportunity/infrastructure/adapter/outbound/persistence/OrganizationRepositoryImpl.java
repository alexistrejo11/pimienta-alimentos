package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OrganizationNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OrganizationRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OrganizationJpaEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizationRepositoryImpl implements OrganizationRepository {

  private final OrganizationJpaRepository jpaRepository;

  public OrganizationRepositoryImpl(OrganizationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Organization> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(OrganizationPersistenceMapper::toDomain);
  }

  @Override
  public Page<Organization> search(OrganizationSearchCriteria criteria, Pageable pageable) {
    return jpaRepository
        .findAll(OrganizationSpecifications.fromCriteria(criteria), pageable)
        .map(OrganizationPersistenceMapper::toDomain);
  }

  @Override
  public Organization save(Organization organization) {
    OrganizationJpaEntity entity;
    if (organization.getId() != null && organization.getId() > 0) {
      entity =
          jpaRepository
              .findById(organization.getId())
              .orElseThrow(() -> new OrganizationNotFoundException(organization.getId()));
    } else {
      entity = new OrganizationJpaEntity();
    }
    OrganizationPersistenceMapper.apply(organization, entity);
    return OrganizationPersistenceMapper.toDomain(jpaRepository.save(entity));
  }
}
