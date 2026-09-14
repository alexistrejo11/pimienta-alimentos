package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "PosTelemetryDeviceResponse")
public record PosTelemetryDeviceResponse(
    UUID id, Long headquarterId, String visibleCode, String deviceName, String status,
    String appVersion, PosHealthSnapshotResponse latestHealth,
    PagedResponse<PosHealthSnapshotResponse> history) {}
