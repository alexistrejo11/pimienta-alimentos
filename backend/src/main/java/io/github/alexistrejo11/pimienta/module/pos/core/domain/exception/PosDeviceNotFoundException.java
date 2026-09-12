package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.UUID;

public class PosDeviceNotFoundException extends ResourceNotFoundException {

  public PosDeviceNotFoundException(UUID id) {
    super(
        ErrorCode.POS_DEVICE_NOT_FOUND,
        "The requested POS device was not found.",
        Map.of("deviceId", id.toString()),
        "POS device not found: id=" + id);
  }
}
