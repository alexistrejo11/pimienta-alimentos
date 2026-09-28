package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String description,
    Unit unit,
    String barcode,
    Status status,
    boolean trackStock,
    Long inventoryItemId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version) {}
