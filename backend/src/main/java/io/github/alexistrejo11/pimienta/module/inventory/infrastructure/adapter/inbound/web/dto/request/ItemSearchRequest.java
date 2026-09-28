package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemKind;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.math.BigDecimal;

/** Query parameters for warehouse item search. */
public class ItemSearchRequest extends PageableRequest {

  private String search;
  private ItemCategory category;
  private ItemStatus status;
  private BigDecimal minCost;
  private BigDecimal maxCost;
  private ItemKind kind;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
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

  public BigDecimal getMinCost() {
    return minCost;
  }

  public void setMinCost(BigDecimal minCost) {
    this.minCost = minCost;
  }

  public BigDecimal getMaxCost() {
    return maxCost;
  }

  public void setMaxCost(BigDecimal maxCost) {
    this.maxCost = maxCost;
  }

  public ItemKind getKind() {
    return kind;
  }

  public void setKind(ItemKind kind) {
    this.kind = kind;
  }

  public ItemSearchCriteria toCriteria() {
    return new ItemSearchCriteria(search, category, status, minCost, maxCost, kind);
  }
}
