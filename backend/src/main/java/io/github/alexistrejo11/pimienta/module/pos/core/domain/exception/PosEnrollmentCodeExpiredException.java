package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosEnrollmentCodeExpiredException extends BusinessValidationException {

  public PosEnrollmentCodeExpiredException(String code) {
    super(
        ErrorCode.POS_ENROLLMENT_CODE_EXPIRED,
        "The enrollment code has expired.",
        Map.of("code", code),
        "POS enrollment code expired: " + code);
  }
}
