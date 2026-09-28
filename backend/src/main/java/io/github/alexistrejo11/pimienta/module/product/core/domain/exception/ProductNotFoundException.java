package io.github.alexistrejo11.pimienta.module.product.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class ProductNotFoundException extends ResourceNotFoundException {

  public ProductNotFoundException(Long id) {
    super(
        ErrorCode.PRODUCT_NOT_FOUND,
        "The requested product was not found.",
        Map.of("productId", id),
        "Product not found: id=" + id);
  }

  public ProductNotFoundException(String skuOrBarcode) {
    super(
        ErrorCode.PRODUCT_NOT_FOUND,
        "The requested product was not found.",
        Map.of("skuOrBarcode", skuOrBarcode),
        "Product not found: skuOrBarcode=" + skuOrBarcode);
  }
}
