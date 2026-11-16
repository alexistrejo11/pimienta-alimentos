package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertOrganizationCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OrganizationResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OrganizationSearchRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpsertOrganizationRequest;

public final class OrganizationWebMapper {

  private OrganizationWebMapper() {}

  public static OrganizationSearchCriteria toCriteria(OrganizationSearchRequest filter) {
    return new OrganizationSearchCriteria(
        filter != null ? filter.getSearch() : null, filter != null ? filter.getKind() : null);
  }

  public static UpsertOrganizationCommand toCommand(UpsertOrganizationRequest request) {
    return new UpsertOrganizationCommand(
        request.name(),
        request.kind(),
        request.notes(),
        request.contactName(),
        request.contactEmail(),
        request.contactPhone());
  }

  public static OrganizationResponse toResponse(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getKind(),
        organization.getNotes(),
        organization.getContactName(),
        organization.getContactEmail(),
        organization.getContactPhone(),
        organization.getCreatedAt(),
        organization.getUpdatedAt());
  }
}
