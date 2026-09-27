package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "SupplierResponse")
public record SupplierResponse(
    long id,
    String name,
    String contactName,
    String phone,
    String brand,
    List<Long> headquarterIds) {}
