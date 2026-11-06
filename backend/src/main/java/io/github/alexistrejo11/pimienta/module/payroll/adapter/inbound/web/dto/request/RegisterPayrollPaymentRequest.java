package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.payroll.core.domain.enums.PayrollFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RegisterPayrollPaymentRequest", description = "Payload to register a payroll payment.")
public record RegisterPayrollPaymentRequest(
    @NotNull @Schema(description = "Employee id.", example = "101")
    Long employeeId,
    @NotNull @Schema(description = "Payroll frequency.", example = "WEEKLY")
    PayrollFrequency frequency,
    @NotNull
        @Schema(description = "Worked days range start.", type = "string", format = "date", example = "2026-04-01")
    LocalDate workedDaysStart,
    @NotNull
        @Schema(description = "Worked days range end.", type = "string", format = "date", example = "2026-04-07")
    LocalDate workedDaysEnd,
    @NotNull @Positive @Schema(description = "Gross amount.", example = "3500.00")
    BigDecimal grossAmount,
    @NotNull @Schema(description = "Net amount sent to employee.", example = "3350.00")
    BigDecimal netAmount,
    @NotBlank @Schema(description = "Destination account number/alias.", example = "012180001234567890")
    String destinationAccount,
    @NotBlank @Schema(description = "Payment transaction id.", example = "TX-2026-000134")
    String transactionId) {}
