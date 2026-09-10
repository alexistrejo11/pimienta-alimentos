package io.github.alexistrejo11.pimienta.module.pos.core.application;

import java.time.Instant;

public record PosReportSummaryRow(
    long headquarterId,
    long salesCentavos,
    long ticketCount,
    long wasteCount,
    long cancellationCount,
    Instant lastShiftClosedAt,
    long openIncidentCount,
    long deviceCount) {}
