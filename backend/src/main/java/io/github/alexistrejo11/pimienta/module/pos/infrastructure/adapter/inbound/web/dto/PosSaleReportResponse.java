package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PosSaleReportResponse")
public record PosSaleReportResponse(
    UUID saleId,
    UUID eventId,
    Long headquarterId,
    UUID deviceId,
    UUID shiftId,
    Long cashierOperatorId,
    String folio,
    long grossCentavos,
    long discountCentavos,
    long totalCentavos,
    String status,
    Instant occurredAt) {}
