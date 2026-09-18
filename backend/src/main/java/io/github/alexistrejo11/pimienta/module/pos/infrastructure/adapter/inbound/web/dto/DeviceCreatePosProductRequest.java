package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "DeviceCreatePosProductRequest")
public record DeviceCreatePosProductRequest(
    @NotBlank @Size(max = 300) String name,
    @NotNull @Min(1) Long salePriceCentavos,
    @NotBlank @Size(max = 64) String saleCategory,
    @Size(max = 64) String barcode,
    Long createdByOperatorId,
    StockPolicy stockPolicy) {}
