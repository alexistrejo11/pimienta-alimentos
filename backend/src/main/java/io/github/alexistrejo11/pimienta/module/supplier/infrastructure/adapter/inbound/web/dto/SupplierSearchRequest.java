package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class SupplierSearchRequest extends PageableRequest {

  private String search;
  private Long headquarterId;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }
}
