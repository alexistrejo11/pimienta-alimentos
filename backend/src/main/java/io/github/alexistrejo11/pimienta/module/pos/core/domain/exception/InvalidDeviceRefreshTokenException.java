package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidDeviceRefreshTokenException extends PimientaException {

  public InvalidDeviceRefreshTokenException(String message) {
    super(
        ErrorCode.INVALID_DEVICE_REFRESH_TOKEN,
        HttpStatus.UNAUTHORIZED,
        message,
        Map.of(),
        message,
        null);
  }
}
