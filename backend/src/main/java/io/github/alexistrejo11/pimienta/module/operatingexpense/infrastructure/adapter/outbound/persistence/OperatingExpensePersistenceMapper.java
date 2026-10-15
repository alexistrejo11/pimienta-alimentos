package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence.entity.OperatingExpenseJpaEntity;
import java.util.ArrayList;
import java.util.List;

public final class OperatingExpensePersistenceMapper {

  private OperatingExpensePersistenceMapper() {}

  public static OperatingExpense toDomain(OperatingExpenseJpaEntity entity) {
    return OperatingExpense.builder()
        .withId(entity.getId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withAmount(entity.getAmount())
        .withCategory(entity.getCategory())
        .withConcept(entity.getConcept())
        .withNotes(entity.getNotes())
        .withOccurredOn(entity.getOccurredOn())
        .withRecordedByUserId(entity.getRecordedByUserId())
        .withAttachmentKeys(entity.getAttachmentKeys())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static void apply(OperatingExpense domain, OperatingExpenseJpaEntity entity) {
    if (domain.getId() != null && domain.getId() > 0) {
      entity.setId(domain.getId());
    }
    entity.setHeadquarterId(domain.getHeadquarterId());
    entity.setAmount(domain.getAmount());
    entity.setCategory(domain.getCategory());
    entity.setConcept(domain.getConcept());
    entity.setNotes(blankToNull(domain.getNotes()));
    entity.setOccurredOn(domain.getOccurredOn());
    entity.setRecordedByUserId(domain.getRecordedByUserId());
    entity.setAttachmentKeys(new ArrayList<>(domain.getAttachmentKeys()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setVersion(domain.getVersion());
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
