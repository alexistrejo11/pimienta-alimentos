package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;

public record DeviceCreatePosProductCommand(
    String name,
    long salePriceCentavos,
    String saleCategory,
    String barcode,
    Long createdByOperatorId,
    StockPolicy stockPolicy) {}
