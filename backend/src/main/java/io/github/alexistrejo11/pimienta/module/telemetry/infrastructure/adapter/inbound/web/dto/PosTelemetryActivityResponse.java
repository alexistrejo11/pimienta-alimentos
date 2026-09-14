package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PosTelemetryActivityResponse")
public record PosTelemetryActivityResponse(
    List<PosHealthSnapshotResponse> items, String nextCursor, boolean hasMore) {}
