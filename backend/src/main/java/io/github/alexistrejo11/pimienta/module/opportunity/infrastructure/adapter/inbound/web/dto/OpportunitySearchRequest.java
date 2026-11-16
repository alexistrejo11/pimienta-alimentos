package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class OpportunitySearchRequest extends PageableRequest {

  private String search;
  private Long organizationId;
  private OpportunityStatus status;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public OpportunityStatus getStatus() {
    return status;
  }

  public void setStatus(OpportunityStatus status) {
    this.status = status;
  }
}
