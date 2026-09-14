package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class InventoryCountInvalidStateException extends ConflictException {

  public InventoryCountInvalidStateException(long sessionId, String detail) {
    super(
        ErrorCode.INVENTORY_COUNT_INVALID_STATE,
        "This operation is not allowed for the current count session state.",
        Map.of("sessionId", sessionId),
        detail);
  }
}
