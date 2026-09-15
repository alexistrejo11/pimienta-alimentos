package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record TransferTransactionRequest(
    String externalReference,
    String notes,
    Long initiatedById,
    @NotEmpty @Valid List<TransferLineRequest> lines) {

  public record TransferLineRequest(
      @Positive long itemId,
      @Positive long fromLocationId,
      @Positive long toLocationId,
      @Positive int quantity,
      @NotNull @jakarta.validation.constraints.DecimalMin("0.0") java.math.BigDecimal unitCost) {}
}
