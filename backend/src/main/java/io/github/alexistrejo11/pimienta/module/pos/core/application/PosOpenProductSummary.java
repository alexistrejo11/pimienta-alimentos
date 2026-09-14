package io.github.alexistrejo11.pimienta.module.pos.core.application;

public record PosOpenProductSummary(
    long openProductCentavos,
    long openProductLineCount,
    long openProductTicketCount,
    long openProductPendingReviewCount) {}
