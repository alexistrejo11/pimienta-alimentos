package io.github.alexistrejo11.pimienta.module.opportunity.core.port.output;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import java.util.List;
import java.util.Optional;

public interface OpportunityRequirementRepository {

  Optional<OpportunityRequirement> findById(long id);

  List<OpportunityRequirement> findByOpportunityId(long opportunityId);

  OpportunityRequirement save(OpportunityRequirement requirement);

  long countMandatoryNotSatisfied(long opportunityId);
}
