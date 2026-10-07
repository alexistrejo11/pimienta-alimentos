package io.github.alexistrejo11.pimienta.module.pos.core.application.query;

import java.time.Instant;
import java.util.UUID;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType;

public record PosReportFilterQuery(
    long headquarterId,
    Instant from,
    Instant to,
    UUID shiftId,
    Long productId,
    String eventType,
    PosSaleLineType lineType,
    boolean openProductsOnly,
    /** Nombre de producto, coincidencia parcial. Vacío si no aplica. */
    String productName,
    /** Solo ventas con un pago de cortesía. */
    boolean courtesyOnly) {}
