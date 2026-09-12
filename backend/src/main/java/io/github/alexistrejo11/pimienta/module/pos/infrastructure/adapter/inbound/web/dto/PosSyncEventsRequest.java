package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(name = "PosSyncEventsRequest")
public record PosSyncEventsRequest(
    @NotEmpty @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<PosSyncEventEnvelopeRequest> events) {

  @Schema(name = "PosSyncEventEnvelopeRequest")
  public record PosSyncEventEnvelopeRequest(
      @NotNull UUID eventId,
      @NotNull String eventType,
      @NotNull Integer schemaVersion,
      @NotNull UUID deviceId,
      @NotNull String siteId,
      @NotNull Long deviceSequence,
      UUID aggregateId,
      UUID shiftId,
      @NotNull Instant occurredAt,
      @NotNull Map<String, Object> payload) {}
}
