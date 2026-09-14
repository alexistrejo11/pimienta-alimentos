package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class PosShiftMaterializationException extends PimientaException {

  public PosShiftMaterializationException(String message, String details) {
    super(ErrorCode.MALFORMED_PAYLOAD, HttpStatus.BAD_REQUEST, message, Map.of(), details, null);
  }
}
