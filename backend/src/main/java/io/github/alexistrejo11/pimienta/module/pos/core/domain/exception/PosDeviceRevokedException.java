package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ForbiddenException;
import java.util.Map;
import java.util.UUID;

public class PosDeviceRevokedException extends ForbiddenException {

  public PosDeviceRevokedException(UUID id) {
    super(
        ErrorCode.POS_DEVICE_REVOKED,
        "This POS device has been revoked.",
        Map.of("deviceId", id.toString()),
        "POS device revoked: id=" + id);
  }
}
