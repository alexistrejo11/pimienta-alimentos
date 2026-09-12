package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class ItemBarcodeConflictException extends ConflictException {

  public ItemBarcodeConflictException(String barcode) {
    super(
        ErrorCode.ITEM_BARCODE_ALREADY_EXISTS,
        "An item with this barcode already exists.",
        Map.of("barcode", barcode),
        "Duplicate barcode: " + barcode);
  }
}
