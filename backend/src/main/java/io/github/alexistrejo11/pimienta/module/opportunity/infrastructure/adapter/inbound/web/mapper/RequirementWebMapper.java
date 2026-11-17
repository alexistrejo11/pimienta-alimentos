package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.CreateRequirementRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.RequirementResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpdateRequirementRequest;

public final class RequirementWebMapper {

  private RequirementWebMapper() {}

  public static CreateRequirementCommand toCreateCommand(CreateRequirementRequest request) {
    return new CreateRequirementCommand(
        request.title(),
        request.category(),
        Boolean.TRUE.equals(request.mandatory()),
        request.dueOn(),
        request.notes(),
        request.sortOrder());
  }

  public static UpdateRequirementCommand toUpdateCommand(UpdateRequirementRequest request) {
    return new UpdateRequirementCommand(
        request.title(),
        request.category(),
        Boolean.TRUE.equals(request.mandatory()),
        request.dueOn(),
        request.notes(),
        request.sortOrder());
  }

  public static RequirementResponse toResponse(OpportunityRequirement requirement) {
    return new RequirementResponse(
        requirement.getId(),
        requirement.getOpportunityId(),
        requirement.getTitle(),
        requirement.getCategory(),
        requirement.isMandatory(),
        requirement.getStatus(),
        requirement.getDueOn(),
        requirement.getNotes(),
        requirement.getSortOrder(),
        requirement.getCreatedAt(),
        requirement.getUpdatedAt());
  }
}
