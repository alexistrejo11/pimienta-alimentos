package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemKind;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ItemSpecifications {

  private ItemSpecifications() {}

  public static Specification<ItemJpaEntity> fromCriteria(ItemSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String term = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("name")), term),
                  cb.like(cb.lower(root.get("category").as(String.class)), term)));
        }
        if (criteria.category() != null) {
          parts.add(cb.equal(root.get("category"), criteria.category()));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status()));
        }
        if (criteria.minCost() != null) {
          parts.add(cb.greaterThanOrEqualTo(root.get("costPrice"), criteria.minCost()));
        }
        if (criteria.maxCost() != null) {
          parts.add(cb.lessThanOrEqualTo(root.get("costPrice"), criteria.maxCost()));
        }
        if (criteria.kind() == ItemKind.PRODUCT) {
          parts.add(cb.isNotNull(root.get("productId")));
        } else if (criteria.kind() == ItemKind.WAREHOUSE) {
          parts.add(cb.isNull(root.get("productId")));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
