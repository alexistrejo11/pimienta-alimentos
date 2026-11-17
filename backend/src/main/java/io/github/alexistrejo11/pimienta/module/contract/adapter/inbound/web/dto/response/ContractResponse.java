package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Contrato tal como lo expone la API (lectura y respuestas de escritura). */
@Schema(
    name = "ContractResponse",
    description =
        """
        Representación de un contrato de negocio. Los textos opcionales nunca son `null` en JSON \
        (cadena vacía). `category` o `termKind` pueden mostrar **UNDEFINED** si faltan en \
        almacenamiento legado.""")
public record ContractResponse(
    @Schema(description = "Identificador persistido; 0 o ausente antes de guardar en borradores.", example = "42")
    Long id,
    @Schema(description = "Nombre o título del contrato.", example = "Contrato suministro 2025")
    String name,
    @Schema(description = "Descripción.", example = "Acuerdo marco con proveedor homologado.")
    String description,
    @Schema(
        description =
            "Categoría: EMPLOYEE, SUPPLIER, CUSTOMER, PARTNER, OTHER, o UNDEFINED si no hay dato.",
        example = "SUPPLIER")
    ContractCategory category,
    @Schema(description = "Empleado vinculado (solo EMPLOYEE).", example = "101")
    Long employeeId,
    @Schema(
        description = "Tipo de plazo: FIXED_TERM, INDEFINITE, o UNDEFINED si no hay dato.",
        example = "FIXED_TERM")
    ContractTermKind termKind,
    @Schema(
        description = "Inicio de vigencia.",
        type = "string",
        format = "date",
        example = "2025-05-01")
    LocalDate effectiveStart,
    @Schema(
        description = "Fin de vigencia (null si indefinido o sin fin en modelo).",
        type = "string",
        format = "date",
        example = "2026-04-30")
    LocalDate effectiveEnd,
    @Schema(
        description = "URL del documento firmado o archivo.",
        example = "https://storage.example.com/contracts/sup-2025-001.pdf")
    String documentUrl,
    @Schema(description = "Términos y condiciones.", example = "Pago a 30 días.")
    String termsAndConditions,
    @Schema(description = "Código de referencia.", example = "CT-SUP-2025-042")
    String referenceCode,
    @Schema(description = "Valor pactado (puede ser null).", example = "250000.00")
    BigDecimal agreedValue,
    @Schema(description = "Moneda ISO 4217; cadena vacía si no aplica.", example = "MXN")
    String currencyCode,
    @Schema(
        description = "Meses de ciclo para renovación automática.",
        example = "12",
        minimum = "1")
    Integer renewalCycleMonths,
    @Schema(description = "Número de renovaciones o prórrogas registradas.", example = "2", minimum = "0")
    int extensionCount,
    @Schema(
        description = "Marca de tiempo de la última renovación o extensión.",
        type = "string",
        format = "date-time",
        example = "2026-01-15T10:30:00")
    LocalDateTime lastRenewedAt,
    @Schema(description = "Creación (audit).", type = "string", format = "date-time")
    LocalDateTime createdAt,
    @Schema(description = "Última actualización (audit).", type = "string", format = "date-time")
    LocalDateTime updatedAt) {}
