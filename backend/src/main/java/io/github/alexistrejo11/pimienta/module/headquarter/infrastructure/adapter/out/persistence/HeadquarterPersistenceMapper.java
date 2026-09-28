package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterJpaEntity;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Maps between persistence {@link HeadquarterJpaEntity} and domain {@link Headquarter}. Null-safe on
 * read paths; {@link #toDomain} returns {@code null} when entity is {@code null}.
 */
@Component
public final class HeadquarterPersistenceMapper {

  private HeadquarterPersistenceMapper() {}

  public Headquarter toDomain(HeadquarterJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return Headquarter.builder()
        .withId(entity.getId())
        .withName(text(entity.getName()))
        .withAddress(text(entity.getAddress()))
        .withDescription(text(entity.getDescription()))
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public HeadquarterJpaEntity toEntity(Headquarter headquarter) {
    Objects.requireNonNull(headquarter, "headquarter");
    HeadquarterJpaEntity entity = new HeadquarterJpaEntity();
    Long domainId = headquarter.getId();
    if (domainId != null && domainId != 0L) {
      entity.setId(domainId);
    }
    entity.setName(blankToNull(headquarter.getName()));
    entity.setAddress(blankToNull(headquarter.getAddress()));
    entity.setDescription(blankToNull(headquarter.getDescription()));
    entity.setCreatedAt(headquarter.getCreatedAt() != null ? headquarter.getCreatedAt() : now());
    entity.setUpdatedAt(headquarter.getUpdatedAt() != null ? headquarter.getUpdatedAt() : now());
    entity.setDeletedAt(headquarter.getDeletedAt());
    entity.setVersion(headquarter.getVersion());
    return entity;
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

  private static java.time.LocalDateTime now() {
    return java.time.LocalDateTime.now();
  }
}
