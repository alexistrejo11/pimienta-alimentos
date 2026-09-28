package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
    @NotBlank @Size(max = 300) String name,
    @Size(max = 4000) String description,
    @NotNull Unit unit,
    @Size(max = 64) String barcode,
    boolean trackStock) {}
