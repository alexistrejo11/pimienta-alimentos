package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record OutTransactionRequest(
    String externalReference,
    String notes,
    @NotNull InventoryExitReason exitReason,
    Long initiatedById,
    @NotEmpty @Valid List<OutLineRequest> lines) {

  public record OutLineRequest(
      long itemId, long locationId, int quantity, @NotNull BigDecimal unitCost) {}
}
