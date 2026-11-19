package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.ProposalKind;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityProposalJpaEntity;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

final class OpportunityProposalPersistenceMapper {

  private OpportunityProposalPersistenceMapper() {}

  static OpportunityProposal toDomain(OpportunityProposalJpaEntity e) {
    return OpportunityProposal.builder()
        .withId(e.getId())
        .withOpportunityId(e.getOpportunityId())
        .withKind(parseKind(e.getKind()))
        .withNotes(e.getNotes())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  static void apply(OpportunityProposal domain, OpportunityProposalJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setOpportunityId(domain.getOpportunityId());
    entity.setKind(domain.getKind().name());
    entity.setNotes(Strings.blankToNull(domain.getNotes()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
  }

  private static ProposalKind parseKind(String raw) {
    if (raw == null || raw.isBlank()) {
      return ProposalKind.UNDEFINED;
    }
    try {
      return ProposalKind.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return ProposalKind.UNDEFINED;
    }
  }
}
