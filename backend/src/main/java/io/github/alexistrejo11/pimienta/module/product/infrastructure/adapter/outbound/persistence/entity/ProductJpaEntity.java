package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Filled by trg_products_assign_sku when omitted on insert. */
  @Column(length = 64)
  private String sku;

  @Column(nullable = false, length = 300)
  private String name;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Unit unit;

  @Column(length = 64)
  private String barcode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Status status;

  @Column(name = "inventory_item_id")
  private Long inventoryItemId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Unit getUnit() {
    return unit;
  }

  public void setUnit(Unit unit) {
    this.unit = unit;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Long getInventoryItemId() {
    return inventoryItemId;
  }

  public void setInventoryItemId(Long inventoryItemId) {
    this.inventoryItemId = inventoryItemId;
  }
}
