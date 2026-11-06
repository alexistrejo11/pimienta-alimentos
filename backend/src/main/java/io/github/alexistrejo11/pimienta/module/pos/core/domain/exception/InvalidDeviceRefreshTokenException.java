package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.UnauthorizedException;
import java.util.Map;

public class InvalidDeviceRefreshTokenException extends UnauthorizedException {

  public InvalidDeviceRefreshTokenException(String message) {
    super(ErrorCode.INVALID_DEVICE_REFRESH_TOKEN, message, Map.of(), message);
  }
}
