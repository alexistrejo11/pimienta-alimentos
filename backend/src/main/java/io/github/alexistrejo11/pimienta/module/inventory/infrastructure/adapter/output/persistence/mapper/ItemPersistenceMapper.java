package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import java.math.BigDecimal;

public final class ItemPersistenceMapper {

  private ItemPersistenceMapper() {}

  public static ItemJpaEntity toJpa(Item domain) {
    ItemJpaEntity entity = new ItemJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setName(domain.getName());
    entity.setDescription(blankToNull(domain.getDescription()));
    entity.setCategory(domain.getCategory());
    entity.setUnit(domain.getUnit());
    entity.setBrand(blankToNull(domain.getBrand()));
    entity.setCostPrice(nz(domain.getCostPrice()));
    entity.setReorderPoint(domain.getReorderPoint());
    entity.setReorderQuantity(domain.getReorderQuantity());
    entity.setStatus(domain.getStatus());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    return entity;
  }

  public static Item toDomain(ItemJpaEntity entity) {
    Item item = new Item();
    item.setId(entity.getId());
    item.setName(entity.getName());
    item.setDescription(entity.getDescription());
    item.setCategory(entity.getCategory());
    item.setUnit(entity.getUnit());
    item.setBrand(entity.getBrand());
    item.setCostPrice(nz(entity.getCostPrice()));
    item.setReorderPoint(entity.getReorderPoint());
    item.setReorderQuantity(entity.getReorderQuantity());
    item.setStatus(entity.getStatus());
    item.setProductId(entity.getProductId());
    item.setSaleSku(entity.getSaleSku());
    item.setCreatedAt(entity.getCreatedAt());
    item.setUpdatedAt(entity.getUpdatedAt());
    item.setDeletedAt(entity.getDeletedAt());
    item.setVersion(entity.getVersion());
    return item;
  }

  private static BigDecimal nz(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
