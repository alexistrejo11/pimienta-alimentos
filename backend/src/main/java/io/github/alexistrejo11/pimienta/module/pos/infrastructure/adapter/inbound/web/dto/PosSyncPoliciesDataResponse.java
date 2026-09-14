package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PosSyncPoliciesDataResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PosSyncPoliciesDataResponse(
    boolean allowNegativeStock,
    boolean allowOpenProducts,
    Integer defaultNegativeStockLimit,
    int staleCatalogWarnHours,
    int staleCatalogBlockHours,
    List<String> openAmountCategories) {}
