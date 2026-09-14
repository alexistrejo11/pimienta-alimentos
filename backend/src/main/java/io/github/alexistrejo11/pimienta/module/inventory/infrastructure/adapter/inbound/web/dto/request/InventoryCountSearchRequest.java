package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.util.List;

public class InventoryCountSearchRequest extends PageableRequest {

  private Long locationId;
  private Status status;
  private Long headquarterId;

  public Long getLocationId() {
    return locationId;
  }

  public void setLocationId(Long locationId) {
    this.locationId = locationId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public InventoryCountSearchCriteria toCriteria(List<Long> headquarterIds) {
    return new InventoryCountSearchCriteria(locationId, status, headquarterIds);
  }
}
