package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierHeadquarterJpaEntity;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.outbound.persistence.entity.SupplierJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SupplierSpecifications {

  private SupplierSpecifications() {}

  public static Specification<SupplierJpaEntity> fromCriteria(SupplierSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));

      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String pattern = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("name")), pattern),
                  cb.like(cb.lower(root.get("phone")), pattern),
                  cb.like(cb.lower(root.get("brand")), pattern)));
        }
        if (criteria.headquarterId() != null) {
          parts.add(servesHeadquarter(root, query.subquery(Integer.class), cb, criteria.headquarterId()));
        }
        if (criteria.scopeHeadquarterIds() != null && !criteria.scopeHeadquarterIds().isEmpty()) {
          List<Predicate> scopeParts = new ArrayList<>();
          for (Long hqId : criteria.scopeHeadquarterIds()) {
            scopeParts.add(servesHeadquarter(root, query.subquery(Integer.class), cb, hqId));
          }
          parts.add(cb.or(scopeParts.toArray(Predicate[]::new)));
        }
      }

      return cb.and(parts.toArray(Predicate[]::new));
    };
  }

  /** Includes inactive links so a paused person can be found and turned back on. */
  private static Predicate servesHeadquarter(
      Root<SupplierJpaEntity> root,
      Subquery<Integer> subquery,
      CriteriaBuilder cb,
      Long headquarterId) {
    Root<SupplierHeadquarterJpaEntity> link = subquery.from(SupplierHeadquarterJpaEntity.class);
    subquery.select(cb.literal(1));
    subquery.where(
        cb.equal(link.get("supplier"), root),
        cb.equal(link.get("id").get("headquarterId"), headquarterId));
    return cb.exists(subquery);
  }
}
