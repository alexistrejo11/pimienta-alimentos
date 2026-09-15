package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryMovementSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.InventoryMovementType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.MovementDirection;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class InventoryMovementSearchRequest extends PageableRequest {

  private InventoryMovementType type;
  private MovementDirection direction;
  private Long itemId;
  private Long locationId;
  private String search;
  private ItemCategory category;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime fromDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime toDate;

  public InventoryMovementType getType() {
    return type;
  }

  public void setType(InventoryMovementType type) {
    this.type = type;
  }

  public MovementDirection getDirection() {
    return direction;
  }

  public void setDirection(MovementDirection direction) {
    this.direction = direction;
  }

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

  public String getSearch() { return search; }
  public void setSearch(String search) { this.search = search; }
  public ItemCategory getCategory() { return category; }
  public void setCategory(ItemCategory category) { this.category = category; }

  public LocalDateTime getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDateTime fromDate) {
    this.fromDate = fromDate;
  }

  public LocalDateTime getToDate() {
    return toDate;
  }

  public void setToDate(LocalDateTime toDate) {
    this.toDate = toDate;
  }

  public InventoryMovementSearchCriteria toCriteria(List<Long> headquarterIds) {
    return new InventoryMovementSearchCriteria(type, direction, itemId, locationId, fromDate, toDate, search, category, headquarterIds);
  }
}
