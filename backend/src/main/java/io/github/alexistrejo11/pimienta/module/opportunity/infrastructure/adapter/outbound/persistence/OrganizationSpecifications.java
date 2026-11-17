package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OrganizationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity.OrganizationJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class OrganizationSpecifications {

  private OrganizationSpecifications() {}

  static Specification<OrganizationJpaEntity> fromCriteria(OrganizationSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String pattern = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("name")), pattern),
                  cb.like(cb.lower(root.get("contactName")), pattern),
                  cb.like(cb.lower(root.get("contactEmail")), pattern)));
        }
        if (criteria.kind() != null) {
          parts.add(cb.equal(root.get("kind"), criteria.kind().name()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
