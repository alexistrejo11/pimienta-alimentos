package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityJpaEntity;

final class OpportunityPersistenceMapper {

  private OpportunityPersistenceMapper() {}

  static Opportunity toDomain(OpportunityJpaEntity e) {
    return Opportunity.builder()
        .withId(e.getId())
        .withOrganizationId(e.getOrganizationId())
        .withTitle(e.getTitle())
        .withStatus(parseStatus(e.getStatus()))
        .withSourceUrl(e.getSourceUrl())
        .withExternalReference(e.getExternalReference())
        .withPublishedOn(e.getPublishedOn())
        .withSubmissionDeadline(e.getSubmissionDeadline())
        .withDecisionOn(e.getDecisionOn())
        .withNotes(e.getNotes())
        .withContractId(e.getContractId())
        .withSubmittedAt(e.getSubmittedAt())
        .withClosedAt(e.getClosedAt())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  static void apply(Opportunity domain, OpportunityJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setOrganizationId(domain.getOrganizationId());
    entity.setTitle(domain.getTitle());
    entity.setStatus(domain.getStatus().name());
    entity.setSourceUrl(blankToNull(domain.getSourceUrl()));
    entity.setExternalReference(blankToNull(domain.getExternalReference()));
    entity.setPublishedOn(domain.getPublishedOn());
    entity.setSubmissionDeadline(domain.getSubmissionDeadline());
    entity.setDecisionOn(domain.getDecisionOn());
    entity.setNotes(blankToNull(domain.getNotes()));
    entity.setContractId(domain.getContractId());
    entity.setSubmittedAt(domain.getSubmittedAt());
    entity.setClosedAt(domain.getClosedAt());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
  }

  private static OpportunityStatus parseStatus(String raw) {
    if (raw == null || raw.isBlank()) {
      return OpportunityStatus.UNDEFINED;
    }
    try {
      return OpportunityStatus.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return OpportunityStatus.UNDEFINED;
    }
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String t = value.strip();
    return t.isEmpty() ? null : t;
  }
}
