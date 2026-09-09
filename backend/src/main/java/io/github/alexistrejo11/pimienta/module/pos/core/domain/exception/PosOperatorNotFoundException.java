package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class PosOperatorNotFoundException extends ResourceNotFoundException {

  public PosOperatorNotFoundException(Long id) {
    super(
        ErrorCode.POS_OPERATOR_NOT_FOUND,
        "The requested POS operator was not found.",
        Map.of("operatorId", id),
        "POS operator not found: id=" + id);
  }
}
