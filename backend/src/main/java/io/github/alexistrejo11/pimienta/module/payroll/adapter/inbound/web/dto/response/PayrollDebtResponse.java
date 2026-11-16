package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PayrollDebtResponse(
    Long id,
    Long employeeId,
    Long payrollRecordId,
    BigDecimal amountOwed,
    String reason,
    boolean settled,
    Instant createdAt,
    Instant settledAt) {}
