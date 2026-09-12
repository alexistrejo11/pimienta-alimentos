package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Update body: only **firstName** and **lastName** are required. Other fields may be null
 * (unchanged) or set. Empty optional identity strings should be omitted/null, not {@code ""}.
 */
@Schema(name = "UpdateEmployeeRequest", description = "Update employee; only firstName and lastName required.")
public record UpdateEmployeeRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Email @Size(max = 320) String email,
    @Size(max = 40) String phone,
    @Size(max = 500) String address,
    @Size(max = 18) String curp,
    @Size(max = 13) String rfc,
    @Size(max = 11) String nss,
    @Size(max = 18) String clabe,
    @Size(max = 120) String position,
    @Size(max = 120) String department,
    ContractType contractType,
    WorkShift workShift,
    @Positive BigDecimal salaryPerWeek,
    BigDecimal bonuses,
    BigDecimal foodVouchers,
    BigDecimal integrationFactor) {}
