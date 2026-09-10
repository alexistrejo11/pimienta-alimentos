package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Bounded batch sent by a POS worker after connectivity is restored. */
public record PosTelemetryBatchRequest(
    @NotNull @Size(max = 50) List<@Valid PosTelemetryLogRequest> events,
    @Valid PosTelemetryHealthRequest health) {}
