package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence.entity.ProductJpaEntity;

public final class ProductPersistenceMapper {

  private ProductPersistenceMapper() {}

  public static ProductJpaEntity toJpa(Product domain) {
    ProductJpaEntity entity = new ProductJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setSku(blankToNull(domain.getSku()));
    entity.setName(domain.getName());
    entity.setDescription(blankToNull(domain.getDescription()));
    entity.setUnit(domain.getUnit());
    entity.setBarcode(blankToNull(domain.getBarcode()));
    entity.setStatus(domain.getStatus());
    entity.setInventoryItemId(domain.getInventoryItemId());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
    return entity;
  }

  public static Product toDomain(ProductJpaEntity entity) {
    return Product.builder()
        .withId(entity.getId())
        .withSku(entity.getSku())
        .withName(entity.getName())
        .withDescription(entity.getDescription())
        .withUnit(entity.getUnit())
        .withBarcode(entity.getBarcode())
        .withStatus(entity.getStatus())
        .withInventoryItemId(entity.getInventoryItemId())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
