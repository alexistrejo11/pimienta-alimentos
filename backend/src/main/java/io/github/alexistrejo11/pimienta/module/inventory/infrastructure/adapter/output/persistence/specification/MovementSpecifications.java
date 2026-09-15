package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryMovementSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryMovementJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.ItemJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class MovementSpecifications {

  private MovementSpecifications() {
  }

  public static Specification<InventoryMovementJpaEntity> fromCriteria(InventoryMovementSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      if (criteria != null) {
        if (criteria.type() != null) {
          parts.add(cb.equal(root.get("type"), criteria.type()));
        }
        if (criteria.direction() != null) {
          parts.add(cb.equal(root.get("direction"), criteria.direction()));
        }
        if (criteria.itemId() != null) {
          parts.add(cb.equal(root.get("itemId"), criteria.itemId()));
        }
        if (criteria.locationId() != null) {
          Long id = criteria.locationId();
          parts.add(
              cb.or(
                  cb.equal(root.get("sourceLocationId"), id),
                  cb.equal(root.get("destinationLocationId"), id)));
        }
        if (criteria.fromDate() != null) {
          parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.fromDate()));
        }
        if (criteria.toDate() != null) {
          parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.toDate()));
        }
        if (criteria.search() != null && !criteria.search().isBlank() || criteria.category() != null) {
          var itemQuery = query.subquery(Long.class);
          var item = itemQuery.from(ItemJpaEntity.class);
          List<Predicate> itemParts = new ArrayList<>();
          itemParts.add(cb.equal(item.get("id"), root.get("itemId")));
          itemParts.add(cb.isNull(item.get("deletedAt")));
          if (criteria.search() != null && !criteria.search().isBlank()) {
            String value = "%" + criteria.search().strip().toLowerCase() + "%";
            itemParts.add(cb.or(cb.like(cb.lower(item.get("name")), value), cb.like(cb.lower(item.get("sku")), value), cb.like(cb.lower(item.get("barcode")), value)));
          }
          if (criteria.category() != null) itemParts.add(cb.equal(item.get("category"), criteria.category()));
          itemQuery.select(item.get("id")).where(itemParts.toArray(Predicate[]::new));
          parts.add(cb.exists(itemQuery));
        }
        if (criteria.headquarterIds() != null) {
          var accessible = query.subquery(Long.class);
          var location = accessible.from(StorageLocationJpaEntity.class);
          accessible.select(location.get("id"));
          accessible.where(cb.and(
              cb.isNull(location.get("deletedAt")),
              location.get("headquarterId").in(criteria.headquarterIds()),
              cb.or(cb.equal(location.get("id"), root.get("sourceLocationId")), cb.equal(location.get("id"), root.get("destinationLocationId")))));
          parts.add(cb.exists(accessible));
          var inaccessible = query.subquery(Long.class);
          var other = inaccessible.from(StorageLocationJpaEntity.class);
          inaccessible.select(other.get("id"));
          inaccessible.where(cb.and(
              cb.isNull(other.get("deletedAt")),
              cb.or(cb.equal(other.get("id"), root.get("sourceLocationId")), cb.equal(other.get("id"), root.get("destinationLocationId"))),
              cb.not(other.get("headquarterId").in(criteria.headquarterIds()))));
          parts.add(cb.not(cb.exists(inaccessible)));
        }
      }
      if (parts.isEmpty()) {
        return cb.conjunction();
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
