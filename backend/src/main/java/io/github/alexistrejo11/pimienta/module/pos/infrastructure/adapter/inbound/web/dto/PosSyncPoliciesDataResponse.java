package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PosSyncPoliciesDataResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PosSyncPoliciesDataResponse(
    boolean allowOpenProducts,
    int staleCatalogWarnHours,
    int staleCatalogBlockHours,
    List<String> openAmountCategories,
    boolean stockless,
    @Schema(example = "1000000", description = "Soft high-sale warning threshold in centavos; 0 disables it.")
        long highSaleWarningCentavos,
    @Schema(example = "100000", description = "Soft high-change (cash tendered) warning threshold in centavos; 0 disables it.")
        long highChangeWarningCentavos) {}
