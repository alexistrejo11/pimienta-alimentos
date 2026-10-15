package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.persistence.entity.OperatingExpenseJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OperatingExpenseSpecifications {

  private OperatingExpenseSpecifications() {}

  public static Specification<OperatingExpenseJpaEntity> fromCriteria(OperatingExpenseSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String pattern = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("concept")), pattern),
                  cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), pattern)));
        }
        if (criteria.headquarterId() != null) {
          parts.add(cb.equal(root.get("headquarterId"), criteria.headquarterId()));
        }
        if (criteria.from() != null) {
          parts.add(cb.greaterThanOrEqualTo(root.get("occurredOn"), criteria.from()));
        }
        if (criteria.to() != null) {
          parts.add(cb.lessThanOrEqualTo(root.get("occurredOn"), criteria.to()));
        }
        if (criteria.category() != null) {
          parts.add(cb.equal(root.get("category"), criteria.category()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
