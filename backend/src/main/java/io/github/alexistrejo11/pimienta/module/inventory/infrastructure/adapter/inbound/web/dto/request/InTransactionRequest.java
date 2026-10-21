package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record InTransactionRequest(
    String externalReference,
    String notes,
    Long initiatedById,
    @NotEmpty @Valid List<InLineRequest> lines) {

  public record InLineRequest(
      long itemId, long locationId, int quantity, @NotNull BigDecimal unitCost) {}
}
