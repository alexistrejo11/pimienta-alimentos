package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;

import java.math.BigDecimal;

public final class ItemPersistenceMapper {

  private ItemPersistenceMapper() {
  }

  public static ItemJpaEntity toJpa(Item domain) {
    ItemJpaEntity e = new ItemJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setSku(domain.getSku());
    e.setName(domain.getName());
    e.setDescription(domain.getDescription());
    e.setCategory(domain.getCategory());
    e.setUnit(domain.getUnit());
    e.setBrand(domain.getBrand());
    e.setBarcode(domain.getBarcode());
    e.setCostPrice(nz(domain.getCostPrice()));
    e.setSalePrice(nz(domain.getSalePrice()));
    e.setReorderPoint(domain.getReorderPoint());
    e.setReorderQuantity(domain.getReorderQuantity());
    e.setStatus(domain.getStatus());
    e.setCatalogRole(domain.getCatalogRole());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);
    return e;
  }

  public static Item toDomain(ItemJpaEntity e) {
    Item item = new Item();
    item.setId(e.getId());
    item.setSku(e.getSku());
    item.setName(e.getName());
    item.setDescription(e.getDescription());
    item.setCategory(e.getCategory());
    item.setUnit(e.getUnit());
    item.setBrand(e.getBrand());
    item.setBarcode(e.getBarcode());
    item.setCostPrice(nz(e.getCostPrice()));
    item.setSalePrice(nz(e.getSalePrice()));
    item.setReorderPoint(e.getReorderPoint());
    item.setReorderQuantity(e.getReorderQuantity());
    item.setStatus(e.getStatus());
    item.setCatalogRole(e.getCatalogRole());
    item.setCreatedAt(e.getCreatedAt());
    item.setUpdatedAt(e.getUpdatedAt());
    item.setDeletedAt(e.getDeletedAt());
    item.setVersion(e.getVersion());
    return item;
  }

  private static BigDecimal nz(BigDecimal v) {
    return v != null ? v : BigDecimal.ZERO;
  }
}
