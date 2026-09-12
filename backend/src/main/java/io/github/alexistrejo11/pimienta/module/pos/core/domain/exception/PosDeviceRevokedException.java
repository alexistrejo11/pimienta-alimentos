package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PosDeviceRevokedException extends PimientaException {

  public PosDeviceRevokedException(UUID id) {
    super(
        ErrorCode.POS_DEVICE_REVOKED,
        HttpStatus.FORBIDDEN,
        "This POS device has been revoked.",
        Map.of("deviceId", id.toString()),
        "POS device revoked: id=" + id,
        null);
  }
}
