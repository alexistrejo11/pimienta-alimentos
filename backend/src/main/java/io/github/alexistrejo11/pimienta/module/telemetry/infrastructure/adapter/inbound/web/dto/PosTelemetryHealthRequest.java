package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Bounded POS health snapshot used for aggregate operational metrics. */
public record PosTelemetryHealthRequest(
    @NotBlank @Size(max = 30) String syncState,
    @Min(0) int pendingEvents,
    @Min(0) long oldestPendingAgeSeconds,
    @NotBlank @Size(max = 40) String appVersion) {}
