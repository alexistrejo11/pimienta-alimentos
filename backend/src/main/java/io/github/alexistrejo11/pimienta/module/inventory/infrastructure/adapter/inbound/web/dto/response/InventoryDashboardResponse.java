package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "InventoryDashboardResponse")
public record InventoryDashboardResponse(
    long skuCount,
    long lowStockCount,
    long outOfStockCount,
    long openCountSessionCount,
    long totalAvailableQuantity,
    BigDecimal totalStockValue) {}
