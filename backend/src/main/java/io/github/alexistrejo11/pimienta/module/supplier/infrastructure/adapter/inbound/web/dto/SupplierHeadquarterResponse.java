package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SupplierHeadquarterResponse")
public record SupplierHeadquarterResponse(
    @Schema(description = "Headquarter served by this person.", example = "1") long headquarterId,
    @Schema(description = "Whether this person currently supplies the brand at the headquarter.")
        boolean active) {}
