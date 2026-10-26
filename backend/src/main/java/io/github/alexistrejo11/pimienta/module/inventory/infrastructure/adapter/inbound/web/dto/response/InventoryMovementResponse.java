package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.InventoryMovementType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.MovementDirection;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryMovementResponse(
    Long id,
    Long itemId,
    String itemSku,
    Long sourceLocationId,
    String sourceLocationCode,
    Long destinationLocationId,
    String destinationLocationCode,
    int quantity,
    BigDecimal unitCost,
    InventoryMovementType type,
    MovementDirection direction,
    InventoryExitReason exitReason,
    String description,
    String referenceNumber,
    Long performedById,
    int stockAfterMovement,
    Long transactionId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version) {}
