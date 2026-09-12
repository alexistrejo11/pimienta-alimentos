package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class PosApkNotFoundException extends ResourceNotFoundException {

  public PosApkNotFoundException(String manifestKey) {
    super(
        ErrorCode.POS_APK_NOT_FOUND,
        "The POS Android APK release was not found.",
        Map.of("manifestKey", manifestKey),
        "POS APK release not found: manifestKey=" + manifestKey);
  }
}
