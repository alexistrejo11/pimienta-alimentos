package io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class HeadquarterItemNotFoundException extends ResourceNotFoundException {

  public HeadquarterItemNotFoundException(long headquarterId, long itemId) {
    super(
        ErrorCode.HEADQUARTER_ITEM_NOT_FOUND,
        "Headquarter POS catalog item was not found.",
        Map.of("headquarterId", headquarterId, "itemId", itemId),
        "HeadquarterItem not found: hq=" + headquarterId + " item=" + itemId);
  }
}
