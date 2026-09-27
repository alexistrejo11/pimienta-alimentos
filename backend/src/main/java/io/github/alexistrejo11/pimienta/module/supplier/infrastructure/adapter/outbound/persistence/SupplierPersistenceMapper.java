package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity;
import java.util.ArrayList;
import java.util.List;

public final class SupplierPersistenceMapper {

  private SupplierPersistenceMapper() {}

  public static Supplier toDomain(SupplierJpaEntity e) {
    List<Long> hqIds = new ArrayList<>();
    if (e.getHeadquarterIds() != null) {
      hqIds.addAll(e.getHeadquarterIds());
    }
    return Supplier.builder()
        .withId(e.getId())
        .withName(e.getName())
        .withContactName(e.getContactName())
        .withPhone(e.getPhone())
        .withBrand(e.getBrand())
        .withHeadquarterIds(hqIds)
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  public static SupplierJpaEntity toJpa(Supplier domain) {
    SupplierJpaEntity e = new SupplierJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setName(domain.getName());
    e.setContactName(domain.getContactName());
    e.setPhone(domain.getPhone());
    e.setBrand(domain.getBrand());
    e.setHeadquarterIds(new java.util.LinkedHashSet<>(domain.getHeadquarterIds()));
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }
}
