package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryStockResponse;

public final class InventoryStockWebMapper {

  private InventoryStockWebMapper() {}

  public static InventoryStockResponse toResponse(Inventory inv) {
    return new InventoryStockResponse(
        inv.getId(),
        inv.getItem().getId(),
        inv.getItem().getSaleSku(),
        inv.getItem().getName(),
        inv.getLocation().getId(),
        inv.getLocation().getCode(),
        inv.getLocation().getName(),
        inv.getAvailableQuantity(),
        inv.getReservedQuantity(),
        inv.getInTransitQuantity(),
        inv.getStatus(),
        inv.getCreatedAt(),
        inv.getUpdatedAt(),
        inv.getDeletedAt(),
        inv.getVersion());
  }
}
