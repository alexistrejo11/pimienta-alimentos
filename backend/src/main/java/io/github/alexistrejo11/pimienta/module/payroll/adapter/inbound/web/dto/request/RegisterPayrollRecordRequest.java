package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RegisterPayrollRecordRequest", description = "Payload to create payroll record for an employee.")
public record RegisterPayrollRecordRequest(
    @NotNull @Schema(description = "Employee id.", example = "101")
    Long employeeId,
    @Schema(description = "Optional payroll period id.", example = "22")
    Long periodId,
    @NotNull
        @Schema(description = "Worked days range start.", type = "string", format = "date", example = "2026-04-01")
    LocalDate workedDaysStart,
    @NotNull
        @Schema(description = "Worked days range end.", type = "string", format = "date", example = "2026-04-07")
    LocalDate workedDaysEnd,
    @NotNull @Positive @Schema(description = "Gross amount.", example = "3500.00")
    BigDecimal grossAmount) {}
