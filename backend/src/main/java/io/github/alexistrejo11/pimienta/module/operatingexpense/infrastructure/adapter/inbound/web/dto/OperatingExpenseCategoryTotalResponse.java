package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "OperatingExpenseCategoryTotalResponse")
public record OperatingExpenseCategoryTotalResponse(
    OperatingExpenseCategory category, BigDecimal totalAmount, long count) {}
