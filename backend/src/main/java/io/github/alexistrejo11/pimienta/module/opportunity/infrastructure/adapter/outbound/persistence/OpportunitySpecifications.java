package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OpportunityJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class OpportunitySpecifications {

  private OpportunitySpecifications() {}

  static Specification<OpportunityJpaEntity> fromCriteria(OpportunitySearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String pattern = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("title")), pattern),
                  cb.like(cb.lower(root.get("externalReference")), pattern),
                  cb.like(cb.lower(root.get("notes")), pattern)));
        }
        if (criteria.organizationId() != null) {
          parts.add(cb.equal(root.get("organizationId"), criteria.organizationId()));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status().name()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
