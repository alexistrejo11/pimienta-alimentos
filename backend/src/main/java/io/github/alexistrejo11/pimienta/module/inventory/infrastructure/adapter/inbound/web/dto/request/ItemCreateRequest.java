package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.CatalogRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemCreateRequest(
    String sku,
    @NotBlank String name,
    String description,
    @NotNull BigDecimal costPrice,
    @NotNull ItemCategory category,
    @NotNull ItemUnit unit,
    @Min(0) int reorderPoint,
    @Min(0) int reorderQuantity,
    String brand,
    String barcode,
    CatalogRole catalogRole) {}
