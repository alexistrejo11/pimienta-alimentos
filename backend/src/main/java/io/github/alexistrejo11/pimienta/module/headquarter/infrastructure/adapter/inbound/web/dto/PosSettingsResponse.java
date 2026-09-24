package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "POS operational settings for a headquarter.")
public record PosSettingsResponse(
    Long id,
    Long headquarterId,
    String currency,
    int catalogStaleWarnHours,
    int catalogStaleBlockHours,
    List<String> openAmountCategories,
    boolean allowOpenProducts,
    Integer defaultNegativeStockLimit,
    boolean stockless,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version) {}
