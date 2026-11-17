package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** POST /api/v1/contracts — alta de contrato comercial o laboral. */
@Schema(
    name = "CreateContractRequest",
    description =
        """
        Cuerpo para registrar un contrato. **EMPLOYEE** exige `employeeId`. **FIXED_TERM** exige \
        `effectiveEnd` posterior a `effectiveStart`. **INDEFINITE** no debe enviar `effectiveEnd`. \
        Si se envía `agreedValue`, `currencyCode` debe ser ISO 4217 de 3 letras (p. ej. MXN).""")
public record CreateContractRequest(
    @NotBlank
        @Schema(
            description = "Nombre o título del contrato (visible en listados).",
            example = "Contrato suministro materias primas 2025")
    String name,
    @Schema(
        description = "Descripción libre; puede omitirse (se interpreta como vacío).",
        example = "Acuerdo marco con proveedor homologado.")
    String description,
    @NotNull
        @Schema(
            description =
                "Tipo de contrato según la contraparte: EMPLOYEE, SUPPLIER, CUSTOMER, PARTNER, INSTITUTIONAL, OTHER.",
            example = "SUPPLIER")
    ContractCategory category,
    @Schema(
        description =
            "Identificador del empleado; **obligatorio** si `category` es EMPLOYEE; debe omitirse en otros tipos.",
        example = "101")
    Long employeeId,
    @NotNull
        @Schema(
            description = "Plazo: FIXED_TERM (con fin) o INDEFINITE (sin fecha de término en sistema).",
            example = "FIXED_TERM")
    ContractTermKind termKind,
    @NotNull
        @Schema(
            description = "Inicio de vigencia (ISO-8601 date).",
            type = "string",
            format = "date",
            example = "2025-05-01")
    LocalDate effectiveStart,
    @Schema(
        description =
            "Fin de vigencia obligatorio para FIXED_TERM; no debe enviarse para INDEFINITE.",
        type = "string",
        format = "date",
        example = "2026-04-30")
    LocalDate effectiveEnd,
    @NotBlank
        @Schema(
            description = "URL del documento firmado o archivo archivado (obligatorio en esta API).",
            example = "https://storage.example.com/contracts/sup-2025-001.pdf")
    String documentUrl,
    @Schema(
        description = "Texto de términos y condiciones (opcional).",
        example = "Pago a 30 días; incumplimiento según legislación aplicable.")
    String termsAndConditions,
    @Schema(description = "Código interno o folio de referencia (opcional).", example = "CT-SUP-2025-042")
    String referenceCode,
    @Schema(
        description =
            "Meses para renovación automática (`POST .../renew`); solo aplica a FIXED_TERM con ciclo definido.",
        example = "12",
        minimum = "1")
    Integer renewalCycleMonths,
    @Schema(
        description = "Valor pactado (opcional). Si se envía, usar también `currencyCode`.",
        example = "250000.00",
        minimum = "0")
    BigDecimal agreedValue,
    @Schema(
        description = "Código de moneda ISO 4217 (3 letras), obligatorio si hay `agreedValue`.",
        example = "MXN",
        minLength = 3,
        maxLength = 3)
    String currencyCode) {}
