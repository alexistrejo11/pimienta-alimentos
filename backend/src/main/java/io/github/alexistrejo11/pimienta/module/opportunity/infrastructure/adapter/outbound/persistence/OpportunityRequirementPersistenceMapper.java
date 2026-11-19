package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityRequirementJpaEntity;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

final class OpportunityRequirementPersistenceMapper {

  private OpportunityRequirementPersistenceMapper() {}

  static OpportunityRequirement toDomain(OpportunityRequirementJpaEntity e) {
    return OpportunityRequirement.builder()
        .withId(e.getId())
        .withOpportunityId(e.getOpportunityId())
        .withTitle(e.getTitle())
        .withCategory(parseCategory(e.getCategory()))
        .withMandatory(e.isMandatory())
        .withStatus(parseStatus(e.getStatus()))
        .withDueOn(e.getDueOn())
        .withNotes(e.getNotes())
        .withSortOrder(e.getSortOrder())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  static void apply(OpportunityRequirement domain, OpportunityRequirementJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setOpportunityId(domain.getOpportunityId());
    entity.setTitle(domain.getTitle());
    entity.setCategory(domain.getCategory().name());
    entity.setMandatory(domain.isMandatory());
    entity.setStatus(domain.getStatus().name());
    entity.setDueOn(domain.getDueOn());
    entity.setNotes(Strings.blankToNull(domain.getNotes()));
    entity.setSortOrder(domain.getSortOrder());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
  }

  private static RequirementCategory parseCategory(String raw) {
    if (raw == null || raw.isBlank()) {
      return RequirementCategory.UNDEFINED;
    }
    try {
      return RequirementCategory.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return RequirementCategory.UNDEFINED;
    }
  }

  private static RequirementStatus parseStatus(String raw) {
    if (raw == null || raw.isBlank()) {
      return RequirementStatus.UNDEFINED;
    }
    try {
      return RequirementStatus.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return RequirementStatus.UNDEFINED;
    }
  }
}
