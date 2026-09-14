package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class InventoryCountNotFoundException extends ResourceNotFoundException {

  public InventoryCountNotFoundException(long sessionId) {
    super(
        ErrorCode.INVENTORY_COUNT_NOT_FOUND,
        "Physical count session not found.",
        Map.of("sessionId", sessionId),
        "Inventory count session not found: " + sessionId);
  }
}
