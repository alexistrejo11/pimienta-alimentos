package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosShiftMaterializationException extends BusinessValidationException {

  public PosShiftMaterializationException(String message, String details) {
    super(ErrorCode.MALFORMED_PAYLOAD, message, Map.of(), details);
  }
}
