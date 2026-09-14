package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryCountSessionJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class InventoryCountSpecifications {

  private InventoryCountSpecifications() {}

  public static Specification<InventoryCountSessionJpaEntity> fromCriteria(
      InventoryCountSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      if (criteria != null) {
        if (criteria.locationId() != null) {
          parts.add(cb.equal(root.get("locationId"), criteria.locationId()));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.headquarterIds() != null) {
          Subquery<Long> locationIds = query.subquery(Long.class);
          Root<StorageLocationJpaEntity> locationRoot = locationIds.from(StorageLocationJpaEntity.class);
          locationIds.select(locationRoot.get("id"));
          locationIds.where(
              cb.and(
                  cb.isNull(locationRoot.get("deletedAt")),
                  locationRoot.get("headquarterId").in(criteria.headquarterIds())));
          parts.add(root.get("locationId").in(locationIds));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
