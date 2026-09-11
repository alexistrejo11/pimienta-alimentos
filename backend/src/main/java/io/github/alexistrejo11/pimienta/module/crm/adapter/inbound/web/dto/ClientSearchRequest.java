package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class ClientSearchRequest extends PageableRequest {

  private String nameContains;

  public String getNameContains() {
    return nameContains;
  }

  public void setNameContains(String nameContains) {
    this.nameContains = nameContains;
  }
}
