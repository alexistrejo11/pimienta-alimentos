package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class ItemHasStockException extends ConflictException {

  public ItemHasStockException(long itemId) {
    super(
        ErrorCode.ITEM_HAS_STOCK,
        "The item still has stock on hand.",
        Map.of("itemId", itemId),
        "Item still has stock on hand: itemId=" + itemId);
  }
}
