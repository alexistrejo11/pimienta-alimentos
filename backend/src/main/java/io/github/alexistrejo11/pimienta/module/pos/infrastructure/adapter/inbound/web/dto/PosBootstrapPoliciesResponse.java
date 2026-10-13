package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapPoliciesResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PosBootstrapPoliciesResponse(
    boolean allowNegativeStock,
    boolean allowOpenProducts,
    Integer defaultNegativeStockLimit,
    int staleCatalogWarnHours,
    int staleCatalogBlockHours,
    boolean stockless,
    @Schema(example = "1000000", description = "Soft high-sale warning threshold in centavos; 0 disables it.")
        long highSaleWarningCentavos,
    @Schema(example = "100000", description = "Soft high-change (cash tendered) warning threshold in centavos; 0 disables it.")
        long highChangeWarningCentavos) {}
