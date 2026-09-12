package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class PosLocationNotFoundException extends ResourceNotFoundException {

  public PosLocationNotFoundException(long headquarterId) {
    super(
        ErrorCode.POS_LOCATION_NOT_FOUND,
        "POS storage location not found for headquarter.",
        Map.of("headquarterId", headquarterId),
        "POS location missing for headquarterId=" + headquarterId);
  }
}
