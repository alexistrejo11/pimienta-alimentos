package io.github.alexistrejo11.pimienta.module.supplier.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;

public class SupplierNotFoundException extends ResourceNotFoundException {

  public SupplierNotFoundException(long id) {
    super(
        ErrorCode.SUPPLIER_NOT_FOUND,
        "The requested supplier was not found.",
        Map.of("supplierId", id),
        "Supplier not found: id=" + id);
  }
}
