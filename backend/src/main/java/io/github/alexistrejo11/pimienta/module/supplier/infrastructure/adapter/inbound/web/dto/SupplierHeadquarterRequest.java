package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "SupplierHeadquarterRequest")
public record SupplierHeadquarterRequest(
    @NotNull @Schema(description = "Headquarter served by this person.", example = "1")
        Long headquarterId,
    @NotNull @Schema(description = "Whether this person currently supplies the brand at the headquarter.")
        Boolean active) {}
