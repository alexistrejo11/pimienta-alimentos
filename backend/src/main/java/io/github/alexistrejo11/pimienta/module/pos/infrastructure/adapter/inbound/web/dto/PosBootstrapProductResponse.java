package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapProductResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PosBootstrapProductResponse(
    String id,
    String sku,
    String barcode,
    String name,
    String saleCategory,
    String unit,
    long priceCentavos,
    long costCentavos,
    boolean available,
    int stockQuantity,
    int stockMinQuantity,
    String stockPolicy,
    Integer negativeStockLimit) {}
