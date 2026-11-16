package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.CreateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.UpdateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.response.ContractResponse;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.CreateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.UpdateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.Contract;

public final class ContractManagerWebMapper {

  private ContractManagerWebMapper() {}

  public static ContractResponse toResponse(Contract c) {
    return new ContractResponse(
        c.getId(),
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

  public static CreateContractCommand toCreateCommand(CreateContractRequest request) {
    return new CreateContractCommand(
        request.name(),
        request.description(),
        request.category(),
        request.employeeId(),
        request.termKind(),
        request.effectiveStart(),
        request.effectiveEnd(),
        request.documentUrl(),
        request.termsAndConditions(),
        request.referenceCode(),
        request.renewalCycleMonths(),
        request.agreedValue(),
        request.currencyCode());
  }

  public static UpdateContractCommand toUpdateCommand(UpdateContractRequest request) {
    return new UpdateContractCommand(
        request.name(),
        request.description(),
        request.category(),
        request.employeeId(),
        request.termKind(),
        request.effectiveStart(),
        request.effectiveEnd(),
        request.documentUrl(),
        request.termsAndConditions(),
        request.referenceCode(),
        request.renewalCycleMonths(),
        request.agreedValue(),
        request.currencyCode());
  }
}
