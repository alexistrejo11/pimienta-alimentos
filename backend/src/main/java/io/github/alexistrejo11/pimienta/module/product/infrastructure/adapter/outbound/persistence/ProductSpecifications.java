package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.outbound.persistence.entity.ProductJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

  private ProductSpecifications() {}

  public static Specification<ProductJpaEntity> fromCriteria(ProductSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.search() != null && !criteria.search().isBlank()) {
          String term = "%" + criteria.search().trim().toLowerCase() + "%";
          parts.add(
              cb.or(
                  cb.like(cb.lower(root.get("name")), term),
                  cb.like(cb.lower(root.get("sku")), term),
                  cb.and(
                      cb.isNotNull(root.get("barcode")),
                      cb.like(cb.lower(root.get("barcode")), term))));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
