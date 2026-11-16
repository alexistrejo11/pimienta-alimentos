package io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class RequirementNotFoundException extends ResourceNotFoundException {

  public RequirementNotFoundException(long id) {
    super(
        ErrorCode.OPPORTUNITY_REQUIREMENT_NOT_FOUND,
        "The requested requirement was not found.",
        Map.of("requirementId", id),
        "Requirement not found: id=" + id);
  }
}
