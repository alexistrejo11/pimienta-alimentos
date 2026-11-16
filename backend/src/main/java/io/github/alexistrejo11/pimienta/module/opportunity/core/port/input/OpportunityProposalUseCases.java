package io.github.alexistrejo11.pimienta.module.opportunity.core.port.input;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertProposalCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import java.util.List;

public interface OpportunityProposalUseCases {

  List<OpportunityProposal> listByOpportunity(long opportunityId);

  OpportunityProposal getById(long opportunityId, long proposalId);

  OpportunityProposal create(long opportunityId, UpsertProposalCommand command);

  OpportunityProposal update(long opportunityId, long proposalId, UpsertProposalCommand command);

  void delete(long opportunityId, long proposalId);
}
