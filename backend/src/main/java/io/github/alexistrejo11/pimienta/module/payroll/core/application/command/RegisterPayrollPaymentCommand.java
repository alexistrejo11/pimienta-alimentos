package io.github.alexistrejo11.pimienta.module.payroll.core.application.command;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterPayrollPaymentCommand(
    Long employeeId,
    PayrollFrequency frequency,
    LocalDate workedDaysStart,
    LocalDate workedDaysEnd,
    BigDecimal grossAmount,
    BigDecimal netAmount,
    String destinationAccount,
    String transactionId) {}
