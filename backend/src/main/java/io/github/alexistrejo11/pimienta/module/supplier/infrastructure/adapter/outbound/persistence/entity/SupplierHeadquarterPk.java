package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SupplierHeadquarterPk implements Serializable {

  @Column(name = "supplier_id", nullable = false)
  private Long supplierId;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  public SupplierHeadquarterPk() {}

  public Long getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(Long supplierId) {
    this.supplierId = supplierId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SupplierHeadquarterPk other)) {
      return false;
    }
    return Objects.equals(supplierId, other.supplierId)
        && Objects.equals(headquarterId, other.headquarterId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(supplierId, headquarterId);
  }
}
