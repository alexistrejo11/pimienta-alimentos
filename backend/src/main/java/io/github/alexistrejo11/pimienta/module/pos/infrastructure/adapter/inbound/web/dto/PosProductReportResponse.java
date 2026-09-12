package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosProductReportResponse")
public record PosProductReportResponse(
    Long productId,
    String productName,
    long quantitySum,
    long subtotalCentavosSum,
    long saleCount) {}
