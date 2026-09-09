package io.github.alexistrejo11.pimienta.module.pos.core.application.query;

import java.time.Instant;
import java.util.UUID;

public record PosReportFilterQuery(
    long headquarterId,
    Instant from,
    Instant to,
    UUID shiftId,
    Long productId,
    String eventType) {}
