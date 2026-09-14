package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.StorageLocationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class StorageLocationSpecifications {

  private StorageLocationSpecifications() {
  }

  public static Specification<StorageLocationJpaEntity> fromCriteria(StorageLocationSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.type() != null) {
          parts.add(cb.equal(root.get("type"), criteria.type()));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.headquarterIds() != null) {
          parts.add(root.get("headquarterId").in(criteria.headquarterIds()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
