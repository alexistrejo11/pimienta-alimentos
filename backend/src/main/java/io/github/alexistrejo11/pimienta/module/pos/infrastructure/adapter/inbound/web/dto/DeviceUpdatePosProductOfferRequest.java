package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "DeviceUpdatePosProductOfferRequest")
public record DeviceUpdatePosProductOfferRequest(
    @NotNull @Min(1) Long salePriceCentavos, @NotNull StockPolicy stockPolicy) {}
