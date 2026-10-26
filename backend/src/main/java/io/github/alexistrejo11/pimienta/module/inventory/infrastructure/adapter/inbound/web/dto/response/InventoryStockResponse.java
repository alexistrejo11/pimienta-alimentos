package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import java.time.LocalDateTime;

public record InventoryStockResponse(
    Long id,
    Long itemId,
    String itemSku,
    String itemName,
    Long locationId,
    String locationCode,
    String locationName,
    int availableQuantity,
    InventoryStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version) {}
