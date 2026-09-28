package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class ProductSearchRequest extends PageableRequest {

  private String search;
  private Status status;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public ProductSearchCriteria toCriteria() {
    return new ProductSearchCriteria(search, status);
  }
}
