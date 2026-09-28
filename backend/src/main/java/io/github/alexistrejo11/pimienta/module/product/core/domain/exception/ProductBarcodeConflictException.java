package io.github.alexistrejo11.pimienta.module.product.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class ProductBarcodeConflictException extends ConflictException {

  public ProductBarcodeConflictException(String barcode) {
    super(
        ErrorCode.PRODUCT_BARCODE_ALREADY_EXISTS,
        "A product with this barcode already exists.",
        Map.of("barcode", barcode),
        "Duplicate product barcode: " + barcode);
  }
}
