package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Upsert POS operational settings for a headquarter.")
public record PosSettingsRequest(
    @Size(max = 8) @Schema(example = "MXN") String currency,
    @Min(1) @Schema(example = "24") Integer catalogStaleWarnHours,
    @Min(1) @Schema(example = "72") Integer catalogStaleBlockHours,
    @Schema(example = "[\"MISC\", \"SERVICE\"]") List<String> openAmountCategories,
    @Schema(example = "true") Boolean allowOpenProducts,
    @Schema(example = "false") Boolean stockless,
    @Min(0)
        @Schema(
            example = "1000000",
            description = "Net sale total in centavos that triggers a soft POS warning; 0 disables it.")
        Long highSaleWarningCentavos,
    @Min(0)
        @Schema(
            example = "100000",
            description = "Cash change in centavos that asks the POS cashier to confirm the tendered amount; 0 disables it.")
        Long highChangeWarningCentavos) {}
