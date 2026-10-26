package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "OperatingExpenseSummaryResponse")
public record OperatingExpenseSummaryResponse(
    @Schema(description = "Sum of amount for the same filters as the list.") BigDecimal totalAmount,
    @Schema(description = "Category with the highest amount. Null when nothing matches.")
        OperatingExpenseCategoryTotalResponse topCategory,
    @Schema(description = "Daily totals ordered by occurredOn ascending. Days without expenses are omitted.")
        List<OperatingExpenseDayTotalResponse> byDay) {}
