package io.github.alexistrejo11.pimienta.module.pos.core.application;

public record PosShiftSalesSummary(
    long saleTicketCount,
    long cashSalesCentavos,
    long cardSalesCentavos,
    long courtesySalesCentavos) {}
