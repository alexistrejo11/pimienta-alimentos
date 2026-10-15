package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "UpsertOperatingExpenseRequest")
public record UpsertOperatingExpenseRequest(
    @Schema(description = "Headquarter that owns the expense. Omitted only when the caller has exactly one assigned sede.", example = "3")
        Long headquarterId,
    @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 12, fraction = 2)
        @Schema(description = "Amount in MXN, greater than zero.", example = "185.50")
        BigDecimal amount,
    @NotNull
        @Schema(description = "Operating cost category. Payroll is not a valid value.", example = "SUPPLIERS_MERCHANDISE")
        OperatingExpenseCategory category,
    @NotBlank
        @Size(max = 200)
        @Schema(description = "Short description of the expense.", example = "Notas de caja")
        String concept,
    @Size(max = 500)
        @Schema(description = "Optional notes.", example = "Compra de hielo")
        String notes,
    @Schema(description = "Business day. Restricted roles may only use today in America/Mexico_City.", example = "2026-10-14")
        LocalDate occurredOn,
    @Schema(description = "On update, keys to keep. Omit to keep every current receipt. Empty list removes them all.")
        List<String> retainAttachmentKeys) {}
