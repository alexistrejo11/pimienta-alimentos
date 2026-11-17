package io.github.alexistrejo11.pimienta.module.opportunity.core.application.query;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;

public record OpportunitySearchCriteria(String search, Long organizationId, OpportunityStatus status) {
  public static OpportunitySearchCriteria empty() {
    return new OpportunitySearchCriteria(null, null, null);
  }
}
