package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemCreateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemUpdateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.ItemResponse;

public final class InventoryItemWebMapper {

  private InventoryItemWebMapper() {}

  public static Item toDomain(ItemCreateRequest request) {
    Item item =
        Item.create(
            request.sku(),
            request.name(),
            request.description() != null ? request.description() : "",
            request.costPrice(),
            request.category(),
            request.unit(),
            request.reorderPoint(),
            request.reorderQuantity());
    if (request.brand() != null) {
      item.setBrand(request.brand());
    }
    if (request.barcode() != null) {
      item.setBarcode(request.barcode());
    }
    item.setCatalogRole(request.catalogRole());
    return item;
  }

  public static Item toMergedDomain(Long id, ItemUpdateRequest request) {
    Item merged = new Item();
    merged.setId(id);
    merged.setSku(request.sku());
    merged.setName(request.name());
    merged.setDescription(request.description() != null ? request.description() : "");
    merged.setCategory(request.category());
    merged.setUnit(request.unit());
    merged.setBrand(request.brand());
    merged.setBarcode(request.barcode());
    merged.setCostPrice(request.costPrice());
    merged.setReorderPoint(request.reorderPoint());
    merged.setReorderQuantity(request.reorderQuantity());
    merged.setStatus(request.status());
    merged.setCatalogRole(request.catalogRole());
    return merged;
  }

  public static ItemResponse toResponse(Item item) {
    return new ItemResponse(
        item.getId(),
        item.getSku(),
        item.getName(),
        item.getDescription(),
        item.getCategory(),
        item.getUnit(),
        item.getBrand(),
        item.getBarcode(),
        item.getCostPrice(),
        item.getReorderPoint(),
        item.getReorderQuantity(),
        item.getStatus(),
        item.getCreatedAt(),
        item.getUpdatedAt(),
        item.getDeletedAt(),
        item.getVersion(),
        item.getCatalogRole());
  }
}
