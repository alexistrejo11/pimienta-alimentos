package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PosEnrollmentCodeInvalidException extends PimientaException {

  public PosEnrollmentCodeInvalidException() {
    super(
        ErrorCode.POS_ENROLLMENT_CODE_INVALID,
        HttpStatus.BAD_REQUEST,
        "The enrollment code is invalid.",
        Map.of(),
        "Invalid POS enrollment code",
        null);
  }
}
