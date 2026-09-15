package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory.InventoryStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import java.math.BigDecimal;

public record GlobalInventoryResponse(Long itemId, String sku, String name, ItemCategory category, Long headquarterId, String headquarterName, long availableQuantity, long reservedQuantity, long inTransitQuantity, long totalQuantity, BigDecimal unitCost, BigDecimal totalValue, InventoryStatus status) {}
