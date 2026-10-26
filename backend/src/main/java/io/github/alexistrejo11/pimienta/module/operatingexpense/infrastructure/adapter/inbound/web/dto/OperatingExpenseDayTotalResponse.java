package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "OperatingExpenseDayTotalResponse")
public record OperatingExpenseDayTotalResponse(LocalDate occurredOn, BigDecimal totalAmount, long count) {}
