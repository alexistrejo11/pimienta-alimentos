package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "PosSyncEventsResponse")
public record PosSyncEventsResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PosSyncEventResultResponse> results) {

  @Schema(name = "PosSyncEventResultResponse")
  public record PosSyncEventResultResponse(
      UUID eventId,
      @Schema(
              allowableValues = {"ACCEPTED", "DUPLICATE", "REQUIRES_REVIEW", "REJECTED"},
              example = "ACCEPTED")
          String status,
      Instant serverReceivedAt,
      UUID incidentId,
      String message) {}
}
