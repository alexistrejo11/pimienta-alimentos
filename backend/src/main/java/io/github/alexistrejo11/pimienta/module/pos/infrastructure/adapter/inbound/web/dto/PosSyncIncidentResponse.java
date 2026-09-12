package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "PosSyncIncidentResponse")
public record PosSyncIncidentResponse(
    UUID id,
    UUID eventId,
    Long headquarterId,
    String reasonCode,
    String detail,
    LocalDateTime acceptedAt,
    Long acceptedBy,
    String acceptLabel,
    String acceptNote,
    LocalDateTime createdAt) {}
