package io.github.alexistrejo11.pimienta.module.opportunity.core.port.output;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import java.util.List;
import java.util.Optional;

public interface OpportunityProposalRepository {

  Optional<OpportunityProposal> findById(long id);

  List<OpportunityProposal> findByOpportunityId(long opportunityId);

  OpportunityProposal save(OpportunityProposal proposal);
}
