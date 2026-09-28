package io.github.alexistrejo11.pimienta.module.product.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class ProductStockRequiredException extends ConflictException {

  public ProductStockRequiredException(long productId) {
    super(
        ErrorCode.PRODUCT_STOCK_REQUIRED,
        "A controlled offer requires a stocked product.",
        Map.of("productId", productId),
        "Controlled catalog row requires inventory item: productId=" + productId);
  }
}
