package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryTransactionResponse(
    Long id,
    String transactionNumber,
    TransactionType type,
    TransactionStatus status,
    String externalReference,
    String notes,
    InventoryExitReason exitReason,
    Long initiatedById,
    Long approvedById,
    LocalDateTime approvedAt,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version,
    List<InventoryMovementResponse> movements) {}
