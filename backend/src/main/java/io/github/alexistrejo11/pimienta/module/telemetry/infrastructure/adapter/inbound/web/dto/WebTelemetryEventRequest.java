package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Safe browser event envelope; it intentionally has no arbitrary payload field. */
public record WebTelemetryEventRequest(
    @Min(1) @Max(10) int schemaVersion,
    @NotBlank @Size(max = 40) String eventType,
    @NotBlank @Pattern(regexp = "ERROR|WARN|INFO") String level,
    @NotBlank @Size(max = 2000) String message,
    @Size(max = 12000) String stack,
    @Size(max = 200) String route,
    @Size(max = 80) String release,
    @Size(max = 120) String traceId,
    @Size(max = 120) String userAgent,
    @Size(max = 500) String occurredAt) {}
