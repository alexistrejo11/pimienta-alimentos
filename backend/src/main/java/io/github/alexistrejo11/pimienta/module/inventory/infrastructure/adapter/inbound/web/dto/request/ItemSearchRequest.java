package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.CatalogRole;
import java.math.BigDecimal;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;

/** Query parameters for item search. */
public class ItemSearchRequest extends PageableRequest {

  private String search;
  private ItemCategory category;
  private ItemStatus status;
  private CatalogRole catalogRole;
  private BigDecimal minCost;
  private BigDecimal maxCost;

  public String getName() {
    return search;
  }

  public void setName(String search) {
    this.search = search;
  }

  public String getSearch() { return search; }
  public void setSearch(String search) { this.search = search; }

  public String getSku() {
    return search;
  }

  public void setSku(String search) {
    this.search = search;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

  public ItemStatus getStatus() {
    return status;
  }

  public void setStatus(ItemStatus status) {
    this.status = status;
  }

  public CatalogRole getCatalogRole() { return catalogRole; }
  public void setCatalogRole(CatalogRole catalogRole) { this.catalogRole = catalogRole; }
  public BigDecimal getMinCost() { return minCost; }
  public void setMinCost(BigDecimal minCost) { this.minCost = minCost; }
  public BigDecimal getMaxCost() { return maxCost; }
  public void setMaxCost(BigDecimal maxCost) { this.maxCost = maxCost; }

  public ItemSearchCriteria toCriteria() {
    return new ItemSearchCriteria(search, category, status, catalogRole, minCost, maxCost);
  }
}
