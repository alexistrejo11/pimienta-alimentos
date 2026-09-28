package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemStockSettingsRequest(
    @NotNull @DecimalMin("0") BigDecimal costPrice,
    @Min(0) int reorderPoint,
    @Min(0) int reorderQuantity,
    @NotNull ItemStatus status) {}
