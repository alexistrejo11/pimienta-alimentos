package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayrollSummaryResponse(
    LocalDate from,
    LocalDate to,
    long employeesPaid,
    BigDecimal totalGross,
    BigDecimal totalNet,
    BigDecimal totalDiscounts,
    BigDecimal totalBonuses,
    BigDecimal totalDebt) {}
