package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class PosDeviceAlreadyEnrolledException extends ConflictException {

  public PosDeviceAlreadyEnrolledException(UUID deviceId) {
    super(
        ErrorCode.POS_DEVICE_ALREADY_ENROLLED,
        "This device is already enrolled.",
        Map.of("deviceId", deviceId.toString()),
        "POS device already enrolled: id=" + deviceId);
  }
}
