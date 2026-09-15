package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.math.BigDecimal;

public class GlobalInventorySearchRequest extends PageableRequest {
  private String search;
  private ItemCategory category;
  private InventoryStatus status;
  private Long headquarterId;
  private BigDecimal minCost;
  private BigDecimal maxCost;
  public String getSearch() { return search; }
  public void setSearch(String search) { this.search = search; }
  public ItemCategory getCategory() { return category; }
  public void setCategory(ItemCategory category) { this.category = category; }
  public InventoryStatus getStatus() { return status; }
  public void setStatus(InventoryStatus status) { this.status = status; }
  public Long getHeadquarterId() { return headquarterId; }
  public void setHeadquarterId(Long headquarterId) { this.headquarterId = headquarterId; }
  public BigDecimal getMinCost() { return minCost; }
  public void setMinCost(BigDecimal minCost) { this.minCost = minCost; }
  public BigDecimal getMaxCost() { return maxCost; }
  public void setMaxCost(BigDecimal maxCost) { this.maxCost = maxCost; }
}
