package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class OrganizationSearchRequest extends PageableRequest {

  private String search;
  private OrganizationKind kind;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public OrganizationKind getKind() {
    return kind;
  }

  public void setKind(OrganizationKind kind) {
    this.kind = kind;
  }
}
