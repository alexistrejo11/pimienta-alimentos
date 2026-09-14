package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PosHealthSnapshotResponse")
public record PosHealthSnapshotResponse(
    UUID id, UUID deviceId, Long headquarterId, String syncState, int pendingEvents,
    long oldestPendingAgeSeconds, String appVersion, Instant receivedAt) {}
