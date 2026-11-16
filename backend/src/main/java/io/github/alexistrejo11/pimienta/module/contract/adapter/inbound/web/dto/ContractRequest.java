package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body for creating or replacing a business contract (POST / PUT). */
public record ContractRequest(
    @NotBlank String name,
    String description,
    @NotNull ContractCategory category,
    Long employeeId,
    @NotNull ContractTermKind termKind,
    @NotNull LocalDate effectiveStart,
    LocalDate effectiveEnd,
    @NotBlank String documentUrl,
    String termsAndConditions,
    String referenceCode,
    Integer renewalCycleMonths,
    BigDecimal agreedValue,
    String currencyCode) {}
