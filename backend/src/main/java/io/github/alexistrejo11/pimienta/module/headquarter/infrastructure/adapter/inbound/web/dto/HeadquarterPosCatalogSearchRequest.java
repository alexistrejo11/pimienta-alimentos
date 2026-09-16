package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

public class HeadquarterPosCatalogSearchRequest extends PageableRequest {
  private String search;
  private String saleCategory;
  private Boolean available;
  private StockPolicy stockPolicy;

  public String getSearch() { return search; }
  public void setSearch(String search) { this.search = search; }
  public String getSaleCategory() { return saleCategory; }
  public void setSaleCategory(String saleCategory) { this.saleCategory = saleCategory; }
  public Boolean getAvailable() { return available; }
  public void setAvailable(Boolean available) { this.available = available; }
  public StockPolicy getStockPolicy() { return stockPolicy; }
  public void setStockPolicy(StockPolicy stockPolicy) { this.stockPolicy = stockPolicy; }
}
