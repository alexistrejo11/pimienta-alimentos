package io.github.alexistrejo11.pimienta.module.supplier.core.application.query;

import java.util.List;

public record SupplierSearchCriteria(String search, Long headquarterId, List<Long> scopeHeadquarterIds) {

  public static SupplierSearchCriteria empty() {
    return new SupplierSearchCriteria(null, null, null);
  }
}
