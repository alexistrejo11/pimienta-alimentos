package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryMovementResponse;

public final class InventoryMovementWebMapper {

  private InventoryMovementWebMapper() {}

  public static InventoryMovementResponse toResponse(InventoryMovement m) {
    StorageLocation src = m.getSourceLocation();
    StorageLocation dst = m.getDestinationLocation();
    return new InventoryMovementResponse(
        m.getId(),
        m.getItem().getId(),
        m.getItem().getSaleSku(),
        src != null ? src.getId() : null,
        src != null ? src.getCode() : null,
        dst != null ? dst.getId() : null,
        dst != null ? dst.getCode() : null,
        m.getQuantity(),
        m.getUnitCost(),
        m.getType(),
        m.getDirection(),
        m.getDescription(),
        m.getReferenceNumber(),
        m.getPerformedById(),
        m.getStockAfterMovement(),
        m.getTransactionId(),
        m.getCreatedAt(),
        m.getUpdatedAt(),
        m.getVersion());
  }
}
