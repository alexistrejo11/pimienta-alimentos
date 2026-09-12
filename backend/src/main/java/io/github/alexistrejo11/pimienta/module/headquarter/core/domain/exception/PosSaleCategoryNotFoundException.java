package io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class PosSaleCategoryNotFoundException extends ResourceNotFoundException {
  public PosSaleCategoryNotFoundException(long id) {
    super(ErrorCode.POS_SALE_CATEGORY_NOT_FOUND, "POS sale category was not found.",
        Map.of("categoryId", id), "POS sale category not found: " + id);
  }
}
