package io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class OrganizationNotFoundException extends ResourceNotFoundException {

  public OrganizationNotFoundException(long id) {
    super(
        ErrorCode.ORGANIZATION_NOT_FOUND,
        "The requested organization was not found.",
        Map.of("organizationId", id),
        "Organization not found: id=" + id);
  }
}
