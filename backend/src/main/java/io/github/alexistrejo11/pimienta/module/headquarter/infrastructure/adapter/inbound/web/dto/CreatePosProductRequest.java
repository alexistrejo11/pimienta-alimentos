package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreatePosProductRequest(
    @NotBlank @Size(max = 300) String name,
    @Size(max = 4000) String description,
    @NotNull @DecimalMin("0") BigDecimal costPrice,
    @NotNull @DecimalMin("0") BigDecimal salePrice,
    ItemCategory category,
    @NotNull ItemUnit unit,
    @Size(max = 120) String brand,
    @Size(max = 64) String barcode,
    @Min(0) int reorderPoint,
    @Min(0) int reorderQuantity,
    @NotNull @Positive Long posSaleCategoryId,
    Boolean available,
    StockPolicy stockPolicy,
    @Min(0) Integer negativeStockLimit) {}
