package io.github.alexistrejo11.pimienta.module.contract.core.application.command;

import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateContractCommand(
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
    Integer renewalCycleMonths,
    BigDecimal agreedValue,
    String currencyCode) {}
