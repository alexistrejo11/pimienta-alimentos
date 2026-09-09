package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PosEnrollmentCodeExpiredException extends PimientaException {

  public PosEnrollmentCodeExpiredException(String code) {
    super(
        ErrorCode.POS_ENROLLMENT_CODE_EXPIRED,
        HttpStatus.BAD_REQUEST,
        "The enrollment code has expired.",
        Map.of("code", code),
        "POS enrollment code expired: " + code,
        null);
  }
}
