package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.VersionedJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.InventoryMovementType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.MovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "inventory_movements",
    indexes = {
      @Index(name = "idx_inventory_movements_item_id", columnList = "item_id"),
      @Index(name = "idx_inventory_movements_transaction_id", columnList = "transaction_id"),
      @Index(name = "idx_inventory_movements_reference_number", columnList = "reference_number"),
      @Index(name = "idx_inventory_movements_source_location_id", columnList = "source_location_id"),
      @Index(
          name = "idx_inventory_movements_destination_location_id",
          columnList = "destination_location_id"),
      @Index(name = "idx_inventory_movements_created_at", columnList = "created_at")
    })
public class InventoryMovementJpaEntity extends VersionedJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "item_id", nullable = false)
  private Long itemId;

  @Column(name = "source_location_id")
  private Long sourceLocationId;

  @Column(name = "destination_location_id")
  private Long destinationLocationId;

  @Column(name = "transaction_id")
  private Long transactionId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
  private BigDecimal unitCost;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InventoryMovementType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private MovementDirection direction;

  @Column(length = 2000)
  private String description;

  @Column(name = "reference_number", length = 120)
  private String referenceNumber;

  @Column(name = "performed_by_id")
  private Long performedById;

  @Column(name = "stock_after_movement", nullable = false)
  private int stockAfterMovement;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

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

  public Long getSourceLocationId() {
    return sourceLocationId;
  }

  public void setSourceLocationId(Long sourceLocationId) {
    this.sourceLocationId = sourceLocationId;
  }

  public Long getDestinationLocationId() {
    return destinationLocationId;
  }

  public void setDestinationLocationId(Long destinationLocationId) {
    this.destinationLocationId = destinationLocationId;
  }

  public Long getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public Long getPerformedById() {
    return performedById;
  }

  public void setPerformedById(Long performedById) {
    this.performedById = performedById;
  }

  public int getStockAfterMovement() {
    return stockAfterMovement;
  }

  public void setStockAfterMovement(int stockAfterMovement) {
    this.stockAfterMovement = stockAfterMovement;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
