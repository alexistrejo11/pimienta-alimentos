package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosEnrollmentCodeConsumedException extends ConflictException {

  public PosEnrollmentCodeConsumedException(String code) {
    super(
        ErrorCode.POS_ENROLLMENT_CODE_CONSUMED,
        "The enrollment code has already been used.",
        Map.of("code", code),
        "POS enrollment code already consumed: " + code);
  }
}
