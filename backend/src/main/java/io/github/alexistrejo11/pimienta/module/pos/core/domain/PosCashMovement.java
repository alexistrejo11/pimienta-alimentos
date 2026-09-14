package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import java.time.Instant;
import java.util.UUID;

public record PosCashMovement(UUID id, UUID shiftId, String type, long amountCentavos, String folio,
                              String reason, Instant occurredAt) {}
