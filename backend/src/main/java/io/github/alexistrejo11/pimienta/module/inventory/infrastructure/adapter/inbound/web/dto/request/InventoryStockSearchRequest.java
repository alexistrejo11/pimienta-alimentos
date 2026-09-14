package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventorySearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.util.List;

public class InventoryStockSearchRequest extends PageableRequest {

  private Long itemId;
  private Long locationId;
  private InventoryStatus status;
  private Long headquarterId;

  public Long getItemId() {
    return itemId;
  }

  public void setItemId(Long itemId) {
    this.itemId = itemId;
  }

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  public InventoryStatus getStatus() {
    return status;
  }

  public void setStatus(InventoryStatus status) {
    this.status = status;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public InventorySearchCriteria toCriteria(List<Long> effectiveHeadquarterIds) {
    return new InventorySearchCriteria(itemId, locationId, status, effectiveHeadquarterIds);
  }
}
