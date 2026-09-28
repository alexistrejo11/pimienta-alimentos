package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "inventory_stock",
    indexes = {
      @Index(name = "idx_inventory_stock_item_location", columnList = "item_id, location_id"),
      @Index(name = "idx_inventory_stock_item_id", columnList = "item_id"),
      @Index(name = "idx_inventory_stock_location_id", columnList = "location_id"),
      @Index(name = "idx_inventory_stock_deleted_at", columnList = "deleted_at")
    })
public class InventoryJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "location_id", nullable = false)
  private Long locationId;

  @Column(name = "available_quantity", nullable = false)
  private int availableQuantity;

  @Column(name = "reserved_quantity", nullable = false)
  private int reservedQuantity;

  @Column(name = "in_transit_quantity", nullable = false)
  private int inTransitQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InventoryStatus status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public int getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(int availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public int getReservedQuantity() {
    return reservedQuantity;
  }

  public void setReservedQuantity(int reservedQuantity) {
    this.reservedQuantity = reservedQuantity;
  }

  public int getInTransitQuantity() {
    return inTransitQuantity;
  }

  public void setInTransitQuantity(int inTransitQuantity) {
    this.inTransitQuantity = inTransitQuantity;
  }

  public InventoryStatus getStatus() {
    return status;
  }

  public void setStatus(InventoryStatus status) {
    this.status = status;
  }
}
