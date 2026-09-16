package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Effective POS catalog row for one item at one headquarter.")
public record HeadquarterPosCatalogItemResponse(
    Long id,
    Long headquarterId,
    Long itemId,
    String itemName,
    String itemSku,
    String itemBarcode,
    String saleCategory,
    BigDecimal salePrice,
    boolean available,
    StockPolicy stockPolicy,
    Integer negativeStockLimit,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version) {}
