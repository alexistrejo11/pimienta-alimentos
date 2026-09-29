package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class PosEnrollmentHeadquarterMismatchException extends ConflictException {

  public PosEnrollmentHeadquarterMismatchException(UUID deviceId, Long headquarterId) {
    super(
        ErrorCode.POS_ENROLLMENT_HEADQUARTER_MISMATCH,
        "The enrollment code belongs to another site.",
        Map.of(
            "deviceId", deviceId.toString(),
            "headquarterId", String.valueOf(headquarterId)),
        "POS enrollment headquarter mismatch: deviceId="
            + deviceId
            + " codeHeadquarterId="
            + headquarterId);
  }
}
