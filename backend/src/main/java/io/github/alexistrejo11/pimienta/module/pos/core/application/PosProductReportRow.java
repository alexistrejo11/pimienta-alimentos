package io.github.alexistrejo11.pimienta.module.pos.core.application;

/**
 * Aggregated product line totals for POS admin reports (ACCEPTED sales only).
 */
public record PosProductReportRow(
    Long productId,
    String productName,
    long quantitySum,
    long subtotalCentavosSum,
    long saleCount) {}
