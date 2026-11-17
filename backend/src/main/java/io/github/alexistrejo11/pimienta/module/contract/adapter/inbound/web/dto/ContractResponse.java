package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
    long id,
    String name,
    String description,
    ContractCategory category,
    Long employeeId,
    ContractTermKind termKind,
    LocalDate effectiveStart,
    LocalDate effectiveEnd,
    String documentUrl,
    String termsAndConditions,
    String referenceCode,
    BigDecimal agreedValue,
    String currencyCode,
    Integer renewalCycleMonths,
    int extensionCount,
    LocalDateTime lastRenewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
