package io.github.alexistrejo11.pimienta.module.crm.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class ClientNotFoundException extends ResourceNotFoundException {

  public ClientNotFoundException(Long id) {
    super(
        ErrorCode.CLIENT_NOT_FOUND,
        "The requested client was not found.",
        Map.of("clientId", id),
        "Client not found: id=" + id);
  }
}
