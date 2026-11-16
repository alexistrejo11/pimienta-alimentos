package io.github.alexistrejo11.pimienta.module.employees.core.application.dto;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;

public record EmployeeExportRow(
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
                EmployeeStatus status,
                String contractType,
                String workShift,
                String salaryPerWeek,
                String bonuses,
                String foodVouchers,
                String integrationFactor) {
}
