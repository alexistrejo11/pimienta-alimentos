package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosEnrollmentCodeInvalidException extends BusinessValidationException {

  public PosEnrollmentCodeInvalidException() {
    super(
        ErrorCode.POS_ENROLLMENT_CODE_INVALID,
        "The enrollment code is invalid.",
        Map.of(),
        "Invalid POS enrollment code");
  }
}
