package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemIdentityCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.UpdateItemStockSettingsCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemCreateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemStockSettingsRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemUpdateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.ItemResponse;

public final class InventoryItemWebMapper {

  private InventoryItemWebMapper() {}

  public static Item toDomain(ItemCreateRequest request) {
    Item item =
        Item.create(
            request.name(),
            request.description(),
            request.costPrice(),
            request.category(),
            request.unit(),
            request.reorderPoint(),
            request.reorderQuantity());
    item.setBrand(request.brand());
    return item;
  }

  public static UpdateItemIdentityCommand toCommand(ItemUpdateRequest request) {
    return new UpdateItemIdentityCommand(
        request.name(), request.description(), request.category(), request.unit(), request.brand());
  }

  public static UpdateItemStockSettingsCommand toCommand(ItemStockSettingsRequest request) {
    return new UpdateItemStockSettingsCommand(
        request.costPrice(), request.reorderPoint(), request.reorderQuantity(), request.status());
  }

  public static ItemResponse toResponse(Item item) {
    return new ItemResponse(
        item.getId(),
        item.getName(),
        item.getDescription(),
        item.getCategory(),
        item.getUnit(),
        item.getBrand(),
        item.getCostPrice(),
        item.getReorderPoint(),
        item.getReorderQuantity(),
        item.getStatus(),
        item.getKind(),
        item.getProductId(),
        item.isLinkedToProduct() ? item.getSaleSku() : null,
        item.getCreatedAt(),
        item.getUpdatedAt(),
        item.getDeletedAt(),
        item.getVersion());
  }
}
