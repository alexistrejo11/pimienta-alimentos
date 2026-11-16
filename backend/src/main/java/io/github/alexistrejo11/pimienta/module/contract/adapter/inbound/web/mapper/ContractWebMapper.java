package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.ContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.ContractResponse;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.CreateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.UpdateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.Contract;

public final class ContractWebMapper {

  private ContractWebMapper() {}

  public static ContractResponse toResponse(Contract c) {
    return new ContractResponse(
        c.getId() != null ? c.getId() : 0L,
        c.getName(),
        c.getDescription(),
        c.getCategory(),
        c.getEmployeeId(),
        c.getTermKind(),
        c.getEffectiveStart(),
        c.getEffectiveEnd(),
        c.getDocumentUrl(),
        c.getTermsAndConditions(),
        c.getReferenceCode(),
        c.getAgreedValue(),
        c.getCurrencyCode(),
        c.getRenewalCycleMonths(),
        c.getExtensionCount(),
        c.getLastRenewedAt(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }

  public static CreateContractCommand toCreateCommand(ContractRequest r) {
    return new CreateContractCommand(
        r.name(),
        r.description(),
        r.category(),
        r.employeeId(),
        r.termKind(),
        r.effectiveStart(),
        r.effectiveEnd(),
        r.documentUrl(),
        r.termsAndConditions(),
        r.referenceCode(),
        r.renewalCycleMonths(),
        r.agreedValue(),
        r.currencyCode());
  }

  public static UpdateContractCommand toUpdateCommand(ContractRequest r) {
    return new UpdateContractCommand(
        r.name(),
        r.description(),
        r.category(),
        r.employeeId(),
        r.termKind(),
        r.effectiveStart(),
        r.effectiveEnd(),
        r.documentUrl(),
        r.termsAndConditions(),
        r.referenceCode(),
        r.renewalCycleMonths(),
        r.agreedValue(),
        r.currencyCode());
  }
}
