package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Creates a warehouse item. Stock settings are optional and default to zero. */
public record ItemCreateRequest(
    @NotBlank String name,
    String description,
    @DecimalMin("0") BigDecimal costPrice,
    @NotNull ItemCategory category,
    @NotNull ItemUnit unit,
    @Min(0) Integer reorderPoint,
    @Min(0) Integer reorderQuantity,
    String brand) {}
