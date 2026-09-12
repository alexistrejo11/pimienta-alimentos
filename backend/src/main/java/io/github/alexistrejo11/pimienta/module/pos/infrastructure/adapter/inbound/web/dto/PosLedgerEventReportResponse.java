package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(name = "PosLedgerEventReportResponse")
public record PosLedgerEventReportResponse(
    UUID eventId,
    String eventType,
    Long headquarterId,
    UUID deviceId,
    UUID shiftId,
    Instant occurredAt,
    Map<String, Object> payload) {}
