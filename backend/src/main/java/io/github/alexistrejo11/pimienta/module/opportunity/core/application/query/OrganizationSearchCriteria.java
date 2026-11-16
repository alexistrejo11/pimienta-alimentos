package io.github.alexistrejo11.pimienta.module.opportunity.core.application.query;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;

public record OrganizationSearchCriteria(String search, OrganizationKind kind) {
  public static OrganizationSearchCriteria empty() {
    return new OrganizationSearchCriteria(null, null);
  }
}
