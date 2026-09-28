package io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class ItemLinkedToProductException extends ConflictException {

  public ItemLinkedToProductException(long itemId, long productId) {
    super(
        ErrorCode.ITEM_LINKED_TO_PRODUCT,
        "This item belongs to a product; edit it from products.",
        Map.of("itemId", itemId, "productId", productId),
        "Item is linked to product: itemId=" + itemId + ", productId=" + productId);
  }
}
