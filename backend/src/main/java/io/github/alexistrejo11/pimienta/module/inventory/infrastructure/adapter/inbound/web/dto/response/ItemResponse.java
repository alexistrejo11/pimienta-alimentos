package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.CatalogRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
    Long id,
    String sku,
    String name,
    String description,
    ItemCategory category,
    ItemUnit unit,
    String brand,
    String barcode,
    BigDecimal costPrice,
    int reorderPoint,
    int reorderQuantity,
    ItemStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version,
    CatalogRole catalogRole) {}
