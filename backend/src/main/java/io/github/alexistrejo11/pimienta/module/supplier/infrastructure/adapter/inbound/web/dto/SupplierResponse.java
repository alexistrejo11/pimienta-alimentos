package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "SupplierResponse")
public record SupplierResponse(
    long id,
    @Schema(description = "Person who supplies the brand.", example = "Juan Pérez") String name,
    @Schema(description = "Phone number.", example = "+528112345678") String phone,
    @Schema(description = "Brand this person supplies.", example = "Marinela") String brand,
    List<SupplierHeadquarterResponse> headquarters) {}
