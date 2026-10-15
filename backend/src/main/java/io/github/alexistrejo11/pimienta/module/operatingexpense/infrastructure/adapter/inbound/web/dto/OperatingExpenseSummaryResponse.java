package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "OperatingExpenseSummaryResponse")
public record OperatingExpenseSummaryResponse(BigDecimal totalAmount) {}
