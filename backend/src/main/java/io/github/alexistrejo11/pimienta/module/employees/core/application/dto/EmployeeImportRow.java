package io.github.alexistrejo11.pimienta.module.employees.core.application.dto;

import java.math.BigDecimal;

/** Fila importada; enums como texto crudo en {@code *Raw}. */
public record EmployeeImportRow(
                int excelRowNumber,
                Long id,
                String firstName,
                String lastName,
                String email,
                String phone,
                String address,
                String curp,
                String rfc,
                String nss,
                String clabe,
                String position,
                String department,
                String statusRaw,
                String contractTypeRaw,
                String workShiftRaw,
                BigDecimal salaryPerWeek,
                BigDecimal bonuses,
                BigDecimal foodVouchers,
                BigDecimal integrationFactor) {
}
