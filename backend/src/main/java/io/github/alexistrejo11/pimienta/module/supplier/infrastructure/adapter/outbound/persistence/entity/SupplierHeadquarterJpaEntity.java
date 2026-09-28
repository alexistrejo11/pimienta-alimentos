package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "supplier_headquarters")
public class SupplierHeadquarterJpaEntity {

  @EmbeddedId private SupplierHeadquarterPk id = new SupplierHeadquarterPk();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @MapsId("supplierId")
  @JoinColumn(name = "supplier_id", nullable = false)
  private SupplierJpaEntity supplier;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public SupplierHeadquarterPk getId() {
    return id;
  }

  public void setId(SupplierHeadquarterPk id) {
    this.id = id != null ? id : new SupplierHeadquarterPk();
  }

  public SupplierJpaEntity getSupplier() {
    return supplier;
  }

  public void setSupplier(SupplierJpaEntity supplier) {
    this.supplier = supplier;
  }

  public Long getHeadquarterId() {
    return id != null ? id.getHeadquarterId() : null;
  }

  public void setHeadquarterId(Long headquarterId) {
    if (id == null) {
      id = new SupplierHeadquarterPk();
    }
    id.setHeadquarterId(headquarterId);
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SupplierHeadquarterJpaEntity other)) {
      return false;
    }
    return Objects.equals(getHeadquarterId(), other.getHeadquarterId());
  }

  @Override
  public int hashCode() {
    Long headquarterId = getHeadquarterId();
    return headquarterId != null ? headquarterId.hashCode() : 0;
  }
}
