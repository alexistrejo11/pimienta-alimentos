package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdjustmentTransactionRequest(
    String externalReference,
    String notes,
    Long initiatedById,
    @NotEmpty @Valid List<AdjustmentLineRequest> lines) {

  public record AdjustmentLineRequest(
      long itemId, long locationId, @Min(0) int countedQuantity, String reason) {}
}
