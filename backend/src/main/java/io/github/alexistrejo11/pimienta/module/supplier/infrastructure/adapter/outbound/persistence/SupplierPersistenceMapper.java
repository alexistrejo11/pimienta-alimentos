package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.SupplierHeadquarterLink;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierHeadquarterJpaEntity;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SupplierPersistenceMapper {

  private SupplierPersistenceMapper() {}

  public static Supplier toDomain(SupplierJpaEntity e) {
    List<SupplierHeadquarterLink> links = new ArrayList<>();
    if (e.getHeadquarters() != null) {
      for (SupplierHeadquarterJpaEntity row : e.getHeadquarters()) {
        if (row.getHeadquarterId() != null) {
          links.add(new SupplierHeadquarterLink(row.getHeadquarterId(), row.isActive()));
        }
      }
    }
    return Supplier.builder()
        .withId(e.getId())
        .withName(e.getName())
        .withPhone(e.getPhone())
        .withBrand(e.getBrand())
        .withHeadquarters(links)
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  public static void apply(Supplier domain, SupplierJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setName(domain.getName());
    entity.setPhone(domain.getPhone());
    entity.setBrand(domain.getBrand());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    syncHeadquarters(domain, entity);
  }

  private static void syncHeadquarters(Supplier domain, SupplierJpaEntity entity) {
    Map<Long, SupplierHeadquarterJpaEntity> existing = new HashMap<>();
    for (SupplierHeadquarterJpaEntity row : entity.getHeadquarters()) {
      if (row.getHeadquarterId() != null) {
        existing.put(row.getHeadquarterId(), row);
      }
    }
    Set<Long> desired = new HashSet<>();
    for (SupplierHeadquarterLink link : domain.getHeadquarters()) {
      desired.add(link.headquarterId());
      SupplierHeadquarterJpaEntity row = existing.get(link.headquarterId());
      if (row == null) {
        row = new SupplierHeadquarterJpaEntity();
        row.setHeadquarterId(link.headquarterId());
        row.setSupplier(entity);
        entity.getHeadquarters().add(row);
      }
      row.setActive(link.active());
    }
    entity.getHeadquarters().removeIf(row -> !desired.contains(row.getHeadquarterId()));
  }
}
