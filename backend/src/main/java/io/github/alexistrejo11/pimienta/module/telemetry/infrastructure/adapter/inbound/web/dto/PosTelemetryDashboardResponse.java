package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosTelemetryDashboardResponse")
public record PosTelemetryDashboardResponse(
    long totalDevices, long onlineDevices, long degradedDevices, long offlineDevices,
    long devicesWithoutSnapshot, long pendingEvents, long openStaleDevices) {}
