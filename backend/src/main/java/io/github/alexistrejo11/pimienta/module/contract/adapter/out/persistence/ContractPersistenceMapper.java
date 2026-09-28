package io.github.alexistrejo11.pimienta.module.contract.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.contract.adapter.out.persistence.model.BusinessContractJpaEntity;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.Contract;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class ContractPersistenceMapper {

  public Contract toDomain(BusinessContractJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return Contract.builder()
        .Id(entity.getId())
        .Name(text(entity.getName()))
        .Description(text(entity.getDescription()))
        .Category(entity.getCategory())
        .EmployeeId(entity.getEmployeeId())
        .OpportunityId(entity.getOpportunityId())
        .ProjectId(entity.getProjectId())
        .TermKind(entity.getTermKind())
        .EffectiveStart(entity.getEffectiveStart())
        .EffectiveEnd(entity.getEffectiveEnd())
        .DocumentUrl(entity.getDocumentUrl())
        .TermsAndConditions(entity.getTermsAndConditions())
        .ReferenceCode(entity.getReferenceCode())
        .RenewalCycleMonths(entity.getRenewalCycleMonths())
        .AgreedValue(entity.getAgreedValue())
        .CurrencyCode(entity.getCurrencyCode())
        .ExtensionCount(entity.getExtensionCount())
        .LastRenewedAt(entity.getLastRenewedAt())
        .CreatedAt(entity.getCreatedAt())
        .UpdatedAt(entity.getUpdatedAt())
        .DeletedAt(entity.getDeletedAt())
        .Version(entity.getVersion())
        .reconstruct();
  }

  public BusinessContractJpaEntity toEntity(Contract domain) {
    Objects.requireNonNull(domain, "domain");
    BusinessContractJpaEntity e = new BusinessContractJpaEntity();
    Long domainId = domain.getId();
    if (domainId != null && domainId != 0L) {
      e.setId(domainId);
    }
    e.setName(text(domain.getName()));
    e.setDescription(text(domain.getDescription()));
    e.setCategory(domain.getCategoryOrNull());
    e.setEmployeeId(domain.getEmployeeId());
    e.setOpportunityId(domain.getOpportunityId());
    e.setProjectId(domain.getProjectId());
    e.setTermKind(domain.getTermKindOrNull());
    e.setEffectiveStart(domain.getEffectiveStart());
    e.setEffectiveEnd(domain.getEffectiveEnd());
    e.setDocumentUrl(blankToNull(domain.getDocumentUrl()));
    e.setTermsAndConditions(blankToNull(domain.getTermsAndConditions()));
    e.setReferenceCode(blankToNull(domain.getReferenceCode()));
    e.setRenewalCycleMonths(domain.getRenewalCycleMonths());
    e.setAgreedValue(domain.getAgreedValue());
    e.setCurrencyCode(currencyToPersist(domain.getCurrencyCode()));
    e.setExtensionCount(domain.getExtensionCount());
    e.setLastRenewedAt(domain.getLastRenewedAt());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  private static String text(String s) {
    return s != null ? s : "";
  }

  private static String blankToNull(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.strip();
  }

  private static String currencyToPersist(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    return code.strip().toUpperCase(java.util.Locale.ROOT);
  }

  private static java.time.LocalDateTime now() {
    return java.time.LocalDateTime.now();
  }
}
