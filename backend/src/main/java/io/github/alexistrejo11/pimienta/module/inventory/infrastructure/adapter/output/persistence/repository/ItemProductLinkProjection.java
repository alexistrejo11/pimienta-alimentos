package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

public interface ItemProductLinkProjection {
  Long getItemId();

  Long getProductId();

  String getSku();
}
