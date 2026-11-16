package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityRequirementJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRequirementJpaRepository
    extends JpaRepository<OpportunityRequirementJpaEntity, Long> {

  Optional<OpportunityRequirementJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  List<OpportunityRequirementJpaEntity> findByOpportunityIdAndDeletedAtIsNullOrderBySortOrderAscIdAsc(
      Long opportunityId);

  @Query(
      """
      select count(r) from OpportunityRequirementJpaEntity r
      where r.opportunityId = :opportunityId
        and r.deletedAt is null
        and r.mandatory = true
        and r.status not in ('DONE', 'WAIVED')
      """)
  long countMandatoryNotSatisfied(@Param("opportunityId") long opportunityId);
}
