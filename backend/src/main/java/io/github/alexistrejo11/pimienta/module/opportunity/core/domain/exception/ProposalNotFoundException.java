package io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class ProposalNotFoundException extends ResourceNotFoundException {

  public ProposalNotFoundException(long id) {
    super(
        ErrorCode.OPPORTUNITY_PROPOSAL_NOT_FOUND,
        "The requested proposal was not found.",
        Map.of("proposalId", id),
        "Proposal not found: id=" + id);
  }
}
