package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryTransactionSearchCriteria(
    TransactionType type,
    TransactionStatus status,
    LocalDateTime fromDate,
    LocalDateTime toDate,
    Long initiatedById,
    List<Long> headquarterIds) {

  public static InventoryTransactionSearchCriteria empty() {
    return new InventoryTransactionSearchCriteria(null, null, null, null, null, null);
  }
}
