package io.github.alexistrejo11.pimienta.module.product.core.application.query;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;

public record ProductSearchCriteria(String search, Status status) {

  public static ProductSearchCriteria empty() {
    return new ProductSearchCriteria(null, null);
  }
}
