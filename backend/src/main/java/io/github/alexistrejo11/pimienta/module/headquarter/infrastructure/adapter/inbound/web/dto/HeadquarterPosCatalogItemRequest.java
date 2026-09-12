package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Upsert effective POS catalog fields for an inventory item at a headquarter.")
public record HeadquarterPosCatalogItemRequest(
    @NotBlank @Size(max = 64) @Schema(example = "BEVERAGE") String saleCategory,
    @NotNull @DecimalMin("0") @Schema(example = "25.50") BigDecimal salePrice,
    @Schema(example = "true") Boolean available,
    @NotNull @Schema(example = "CONTROLLED") StockPolicy stockPolicy,
    @Schema(example = "5") Integer negativeStockLimit) {}
