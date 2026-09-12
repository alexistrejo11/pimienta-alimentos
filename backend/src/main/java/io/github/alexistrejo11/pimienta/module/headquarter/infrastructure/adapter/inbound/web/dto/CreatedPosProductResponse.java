package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import java.math.BigDecimal;

public record CreatedPosProductResponse(
    Long id, String sku, String name, String barcode, ItemCategory category, ItemUnit unit,
    BigDecimal costPrice, BigDecimal salePrice, Long headquarterId, Long posSaleCategoryId,
    String saleCategory, boolean available, StockPolicy stockPolicy) {}
