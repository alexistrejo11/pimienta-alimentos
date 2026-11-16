package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertProposalCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.ProposalResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpsertProposalRequest;

public final class ProposalWebMapper {

  private ProposalWebMapper() {}

  public static UpsertProposalCommand toCommand(UpsertProposalRequest request) {
    return new UpsertProposalCommand(request.kind(), request.notes());
  }

  public static ProposalResponse toResponse(OpportunityProposal proposal) {
    return new ProposalResponse(
        proposal.getId(),
        proposal.getOpportunityId(),
        proposal.getKind(),
        proposal.getNotes(),
        proposal.getCreatedAt(),
        proposal.getUpdatedAt());
  }
}
