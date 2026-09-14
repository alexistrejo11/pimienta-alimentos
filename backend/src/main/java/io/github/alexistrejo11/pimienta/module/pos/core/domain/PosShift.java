package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import java.time.Instant;
import java.util.UUID;

public record PosShift(
    UUID id,
    long headquarterId,
    UUID deviceId,
    Long cashierOperatorId,
    long openingCashCentavos,
    Instant openedAt,
    Instant closedAt,
    String status,
    Long expectedCashCentavos,
    Long countedCashCentavos,
    Long differenceCentavos) {}
