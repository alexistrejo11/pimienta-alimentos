package io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;

public class PosSaleCategoryConflictException extends ConflictException {
  public PosSaleCategoryConflictException(String name) {
    super(ErrorCode.POS_SALE_CATEGORY_ALREADY_EXISTS, "A POS sale category with this name already exists.",
        Map.of("name", name), "Duplicate POS sale category: " + name);
  }
  public PosSaleCategoryConflictException(long id) {
    super(ErrorCode.POS_SALE_CATEGORY_IN_USE, "The POS sale category is still in use.",
        Map.of("categoryId", id), "POS sale category is in use: " + id);
  }
}
