package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeOnboardingPhase;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta onboarding: parte **`employee`** (`multipart/form-data`, JSON UTF‑8); el servidor también
 * tolera esa parte etiquetada como binario ({@code application/octet-stream}). Alternativa opcional:
 * mismo cuerpo con {@code POST} {@code application/json}. Solo **firstName** y **lastName** son
 * obligatorios; el resto puede omitirse o ir null.
 */
@Schema(
    name = "RegisterEmployeeRequest",
    description =
        "Payload to register a new employee. Only firstName and lastName are required.")
public record RegisterEmployeeRequest(
    @NotBlank
        @Size(max = 100)
        @Schema(description = "First name.", example = "María", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,
    @NotBlank
        @Size(max = 100)
        @Schema(
            description = "Last name.",
            example = "López García",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,
    @Email
        @Size(max = 320)
        @Schema(description = "Work email (optional).", example = "maria.lopez@empresa.example")
        String email,
    @Size(max = 40) @Schema(description = "Contact phone (optional).", example = "+52 55 1234 5678")
        String phone,
    @Size(max = 500) @Schema(description = "Street address (optional).", example = "Av. Reforma 123, CDMX")
        String address,
    @Size(max = 18)
        @Schema(description = "CURP (optional).", example = "XXXX850101HDFXXX09")
        String curp,
    @Size(max = 13)
        @Schema(description = "RFC (optional).", example = "XAXX010101000")
        String rfc,
    @Size(max = 11)
        @Schema(description = "IMSS NSS (optional).", example = "12345678901")
        String nss,
    @Size(max = 18)
        @Schema(description = "CLABE (optional).", example = "012180001234567890")
        String clabe,
    @Size(max = 32) @Schema(description = "Internal employee number (optional).", example = "EMP-1042")
        String employeeNumber,
    @Size(max = 120) @Schema(description = "Job title (optional).", example = "Operador de línea")
        String position,
    @Size(max = 120) @Schema(description = "Department (optional).", example = "Producción")
        String department,
    @Schema(description = "Employment contract type (optional).") ContractType contractType,
    @Schema(description = "Work shift (optional).") WorkShift workShift,
    @Positive
        @Schema(description = "Gross weekly salary in MXN (optional).", example = "3500.00")
        BigDecimal salaryPerWeek,
    @Schema(description = "Date of birth (optional).", example = "1985-01-01", type = "string", format = "date")
        LocalDate birthDate,
    @Schema(
            description =
                "Onboarding phase (optional): DRAFT or PENDING_CONTRACT. Defaults to DRAFT when omitted.")
        EmployeeOnboardingPhase onboardingPhase) {}
