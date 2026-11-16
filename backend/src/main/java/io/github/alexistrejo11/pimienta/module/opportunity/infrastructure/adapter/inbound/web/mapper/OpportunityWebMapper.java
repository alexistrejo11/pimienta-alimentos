package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.CreateOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OpportunityResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OpportunitySearchRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpdateOpportunityRequest;

public final class OpportunityWebMapper {

  private OpportunityWebMapper() {}

  public static OpportunitySearchCriteria toCriteria(OpportunitySearchRequest filter) {
    return new OpportunitySearchCriteria(
        filter != null ? filter.getSearch() : null,
        filter != null ? filter.getOrganizationId() : null,
        filter != null ? filter.getStatus() : null);
  }

  public static CreateOpportunityCommand toCreateCommand(CreateOpportunityRequest request) {
    return new CreateOpportunityCommand(
        request.organizationId(),
        request.title(),
        request.sourceUrl(),
        request.externalReference(),
        request.publishedOn(),
        request.submissionDeadline(),
        request.decisionOn(),
        request.notes());
  }

  public static UpdateOpportunityCommand toUpdateCommand(UpdateOpportunityRequest request) {
    return new UpdateOpportunityCommand(
        request.organizationId(),
        request.title(),
        request.sourceUrl(),
        request.externalReference(),
        request.publishedOn(),
        request.submissionDeadline(),
        request.decisionOn(),
        request.notes());
  }

  public static OpportunityResponse toResponse(Opportunity opportunity) {
    return new OpportunityResponse(
        opportunity.getId(),
        opportunity.getOrganizationId(),
        opportunity.getTitle(),
        opportunity.getStatus(),
        opportunity.getSourceUrl(),
        opportunity.getExternalReference(),
        opportunity.getPublishedOn(),
        opportunity.getSubmissionDeadline(),
        opportunity.getDecisionOn(),
        opportunity.getNotes(),
        opportunity.getContractId(),
        opportunity.getSubmittedAt(),
        opportunity.getClosedAt(),
        opportunity.getCreatedAt(),
        opportunity.getUpdatedAt());
  }
}
