package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
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
    @Size(max = 64) String barcode,
    Unit unit,
    @NotNull @DecimalMin("0") BigDecimal salePrice,
    @NotNull @Positive Long posSaleCategoryId,
    Boolean available,
    StockPolicy stockPolicy,
    @Min(0) Integer negativeStockLimit) {}
