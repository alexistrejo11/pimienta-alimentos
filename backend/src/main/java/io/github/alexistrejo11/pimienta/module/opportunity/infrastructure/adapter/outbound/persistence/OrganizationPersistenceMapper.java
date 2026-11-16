package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Organization;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OrganizationJpaEntity;

final class OrganizationPersistenceMapper {

  private OrganizationPersistenceMapper() {}

  static Organization toDomain(OrganizationJpaEntity e) {
    return Organization.builder()
        .withId(e.getId())
        .withName(e.getName())
        .withKind(parseKind(e.getKind()))
        .withNotes(e.getNotes())
        .withContactName(e.getContactName())
        .withContactEmail(e.getContactEmail())
        .withContactPhone(e.getContactPhone())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  static void apply(Organization domain, OrganizationJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setName(domain.getName());
    entity.setKind(domain.getKind().name());
    entity.setNotes(blankToNull(domain.getNotes()));
    entity.setContactName(blankToNull(domain.getContactName()));
    entity.setContactEmail(blankToNull(domain.getContactEmail()));
    entity.setContactPhone(blankToNull(domain.getContactPhone()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
  }

  private static OrganizationKind parseKind(String raw) {
    if (raw == null || raw.isBlank()) {
      return OrganizationKind.UNDEFINED;
    }
    try {
      return OrganizationKind.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return OrganizationKind.UNDEFINED;
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
