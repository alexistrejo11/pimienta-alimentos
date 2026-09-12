package io.github.alexistrejo11.pimienta.module.crm.core.application.query;

public record ClientSearchCriteria(String nameContains) {

  public static ClientSearchCriteria empty() {
    return new ClientSearchCriteria(null);
  }
}
