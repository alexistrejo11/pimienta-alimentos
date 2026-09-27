package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(name = "UpsertSupplierRequest")
public record UpsertSupplierRequest(
    @NotBlank @Schema(description = "Business or vendor name.", example = "Distribuidora Norte")
        String name,
    @NotBlank @Schema(description = "Contact person.", example = "Juan Pérez") String contactName,
    @NotBlank @Schema(description = "Phone number.", example = "+528112345678") String phone,
    @NotBlank @Schema(description = "Parent brand served.", example = "Marinela") String brand,
    @NotNull @Schema(description = "Headquarters served by this supplier.")
        List<Long> headquarterIds) {}
