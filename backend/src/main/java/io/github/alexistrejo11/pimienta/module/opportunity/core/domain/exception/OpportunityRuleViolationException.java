package io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.util.Map;

public class OpportunityRuleViolationException extends BusinessValidationException {

  public OpportunityRuleViolationException(
      String clientMessage, Map<String, Object> context, String logDetails) {
    super(clientMessage, context, logDetails);
  }
}
