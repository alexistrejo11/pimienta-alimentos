package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** PUT /api/v1/contracts/{id} — reemplazo completo del contrato. */
@Schema(
    name = "UpdateContractRequest",
    description =
        """
        Misma forma y reglas que el alta (**CreateContractRequest**): actualización completa del \
        estado expuesto por la API. Requiere `Authorization: Bearer <token>`.""")
public record UpdateContractRequest(
    @NotBlank
        @Schema(
            description = "Nombre o título del contrato.",
            example = "Contrato suministro materias primas 2025 (enmienda 1)")
    String name,
    @Schema(
        description = "Descripción libre; puede omitirse.",
        example = "Acuerdo marco con proveedor homologado — anexo logística.")
    String description,
    @NotNull
        @Schema(
            description = "Tipo de contrato: EMPLOYEE, SUPPLIER, CUSTOMER, PARTNER, OTHER.",
            example = "SUPPLIER")
    ContractCategory category,
    @Schema(
        description = "Empleado vinculado; obligatorio si `category` es EMPLOYEE.",
        example = "101")
    Long employeeId,
    @NotNull
        @Schema(description = "Plazo: FIXED_TERM o INDEFINITE.", example = "FIXED_TERM")
    ContractTermKind termKind,
    @NotNull
        @Schema(
            description = "Inicio de vigencia (ISO-8601 date).",
            type = "string",
            format = "date",
            example = "2025-05-01")
    LocalDate effectiveStart,
    @Schema(
        description = "Fin de vigencia (FIXED_TERM); omitir en INDEFINITE.",
        type = "string",
        format = "date",
        example = "2026-10-31")
    LocalDate effectiveEnd,
    @NotBlank
        @Schema(
            description = "URL del documento firmado o archivo archivado.",
            example = "https://storage.example.com/contracts/sup-2025-001-v2.pdf")
    String documentUrl,
    @Schema(
        description = "Términos y condiciones (opcional).",
        example = "Pago a 45 días a partir de recepción de factura.")
    String termsAndConditions,
    @Schema(description = "Código de referencia (opcional).", example = "CT-SUP-2025-042-R1")
    String referenceCode,
    @Schema(
        description = "Meses entre renovaciones automáticas (FIXED_TERM).",
        example = "12",
        minimum = "1")
    Integer renewalCycleMonths,
    @Schema(description = "Valor pactado (opcional).", example = "275000.00", minimum = "0")
    BigDecimal agreedValue,
    @Schema(description = "Moneda ISO 4217 si hay valor pactado.", example = "MXN", minLength = 3, maxLength = 3)
    String currencyCode) {}
