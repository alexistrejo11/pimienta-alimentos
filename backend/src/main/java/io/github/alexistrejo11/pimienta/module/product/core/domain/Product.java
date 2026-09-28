package io.github.alexistrejo11.pimienta.module.product.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;

/**
 * Sale catalog row. The SKU is assigned by the database on insert. Stock, when tracked, is the
 * linked inventory item.
 */
public class Product extends BaseDomain<Long> {

  public enum Unit {
    PIECE,
    KG,
    GRAM,
    LITER,
    ML,
    BOX,
    DOZEN,
    METER,
    SQUARE_METER
  }

  public enum Status {
    ACTIVE,
    DISCONTINUED
  }

  private String sku;
  private String name;
  private String description;
  private Unit unit;
  private String barcode;
  private Status status;
  private Long inventoryItemId;

  private Product() {
    this.id = 0L;
    this.sku = "";
    this.name = "";
    this.description = "";
    this.unit = Unit.PIECE;
    this.status = Status.ACTIVE;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  public String getSku() {
    return sku != null ? sku : "";
  }

  public String getName() {
    return name != null ? name : "";
  }

  public String getDescription() {
    return description != null ? description : "";
  }

  public Unit getUnit() {
    return unit != null ? unit : Unit.PIECE;
  }

  public String getBarcode() {
    return barcode;
  }

  public Status getStatus() {
    return status != null ? status : Status.ACTIVE;
  }

  public Long getInventoryItemId() {
    return inventoryItemId;
  }

  public boolean isTrackStock() {
    return inventoryItemId != null;
  }

  public void setSku(String sku) {
    this.sku = sku != null ? sku : "";
  }

  public void setName(String name) {
    this.name = name != null ? name.strip() : "";
  }

  public void setDescription(String description) {
    this.description = description != null ? description : "";
  }

  public void setUnit(Unit unit) {
    this.unit = unit != null ? unit : Unit.PIECE;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public void setStatus(Status status) {
    this.status = status != null ? status : Status.ACTIVE;
  }

  public void setInventoryItemId(Long inventoryItemId) {
    this.inventoryItemId = inventoryItemId;
  }

  public static final class SafeBuilder {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private Unit unit;
    private String barcode;
    private Status status;
    private Long inventoryItemId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withSku(String sku) {
      this.sku = sku;
      return this;
    }

    public SafeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public SafeBuilder withUnit(Unit unit) {
      this.unit = unit;
      return this;
    }

    public SafeBuilder withBarcode(String barcode) {
      this.barcode = barcode;
      return this;
    }

    public SafeBuilder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withInventoryItemId(Long inventoryItemId) {
      this.inventoryItemId = inventoryItemId;
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public Product reconstruct() {
      Product product = new Product();
      product.id = id != null ? id : 0L;
      product.sku = sku != null ? sku : "";
      product.name = name != null ? name.strip() : "";
      product.description = description != null ? description : "";
      product.unit = unit != null ? unit : Unit.PIECE;
      product.barcode = blankToNull(barcode);
      product.status = status != null ? status : Status.ACTIVE;
      product.inventoryItemId = inventoryItemId;
      product.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      product.updatedAt = updatedAt != null ? updatedAt : product.createdAt;
      product.deletedAt = deletedAt;
      product.version = version;
      return product;
    }

    public Product register() {
      Product product = reconstruct();
      LocalDateTime now = LocalDateTime.now();
      product.id = 0L;
      product.sku = "";
      product.createdAt = now;
      product.updatedAt = now;
      product.deletedAt = null;
      product.version = null;
      return product;
    }

    private static String blankToNull(String value) {
      if (value == null || value.isBlank()) {
        return null;
      }
      return value.strip();
    }
  }
}
