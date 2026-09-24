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
    boolean stockless) {}
