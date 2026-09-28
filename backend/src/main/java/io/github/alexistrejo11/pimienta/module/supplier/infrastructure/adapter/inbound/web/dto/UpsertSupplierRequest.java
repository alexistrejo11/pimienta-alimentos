package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(name = "UpsertSupplierRequest")
public record UpsertSupplierRequest(
    @NotBlank
        @Size(max = 200)
        @Schema(description = "Person who supplies the brand.", example = "Juan Pérez")
        String name,
    @NotBlank
        @Size(max = 40)
        @Schema(description = "Phone number.", example = "+528112345678")
        String phone,
    @NotBlank
        @Size(max = 120)
        @Schema(description = "Brand this person supplies.", example = "Marinela")
        String brand,
    @NotNull @Schema(description = "Headquarters this person supplies, and whether each one is current.")
        List<@Valid SupplierHeadquarterRequest> headquarters) {}
