package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemKind;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ItemResponse(
    Long id,
    String name,
    String description,
    ItemCategory category,
    ItemUnit unit,
    String brand,
    BigDecimal costPrice,
    int reorderPoint,
    int reorderQuantity,
    ItemStatus status,
    ItemKind kind,
    Long productId,
    String saleSku,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version) {}
