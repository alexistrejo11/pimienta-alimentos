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
    @Schema(example = "10") Integer defaultNegativeStockLimit,
    @Schema(example = "false") Boolean stockless) {}
