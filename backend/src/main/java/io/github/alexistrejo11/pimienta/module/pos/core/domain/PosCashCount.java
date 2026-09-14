package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import java.time.Instant;
import java.util.UUID;

public record PosCashCount(UUID id, UUID shiftId, long totalCentavos, String denominations,
                           Instant submittedAt) {}
