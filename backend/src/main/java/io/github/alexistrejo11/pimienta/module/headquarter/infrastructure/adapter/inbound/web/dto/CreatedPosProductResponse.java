package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import java.math.BigDecimal;

public record CreatedPosProductResponse(
    Long id,
    String sku,
    String name,
    String barcode,
    Unit unit,
    boolean trackStock,
    BigDecimal salePrice,
    Long headquarterId,
    Long posSaleCategoryId,
    String saleCategory,
    boolean available,
    StockPolicy stockPolicy) {}
