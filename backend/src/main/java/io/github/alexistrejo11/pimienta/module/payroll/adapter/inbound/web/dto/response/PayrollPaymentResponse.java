package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollRecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PayrollPaymentResponse(
    Long id,
    Long employeeId,
    PayrollFrequency frequency,
    LocalDate workedDaysStart,
    LocalDate workedDaysEnd,
    BigDecimal grossAmount,
    BigDecimal netAmount,
    String destinationAccount,
    String transactionId,
    PayrollRecordStatus status,
    BigDecimal pendingAmount,
    Instant createdAt) {}
