package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryTransactionSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryTransactionJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryMovementJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

  private TransactionSpecifications() {
  }

  public static Specification<InventoryTransactionJpaEntity> fromCriteria(
      InventoryTransactionSearchCriteria criteria) {
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
        if (criteria.fromDate() != null) {
          parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.fromDate()));
        }
        if (criteria.toDate() != null) {
          parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.toDate()));
        }
        if (criteria.initiatedById() != null) {
          parts.add(cb.equal(root.get("initiatedById"), criteria.initiatedById()));
        }
        if (criteria.headquarterIds() != null) {
          var movementExists = query.subquery(Long.class);
          var movement = movementExists.from(InventoryMovementJpaEntity.class);
          movementExists.select(movement.get("id"));
          movementExists.where(cb.equal(movement.get("transactionId"), root.get("id")));

          var inaccessibleSource = query.subquery(Long.class);
          var sourceMovement = inaccessibleSource.from(InventoryMovementJpaEntity.class);
          var sourceLocation = query.subquery(Long.class);
          var source = sourceLocation.from(StorageLocationJpaEntity.class);
          sourceLocation.select(source.get("id"));
          sourceLocation.where(
              cb.and(
                  cb.equal(source.get("id"), sourceMovement.get("sourceLocationId")),
                  cb.isNull(source.get("deletedAt")),
                  source.get("headquarterId").in(criteria.headquarterIds())));
          inaccessibleSource.select(sourceMovement.get("id"));
          inaccessibleSource.where(
              cb.and(
                  cb.equal(sourceMovement.get("transactionId"), root.get("id")),
                  cb.isNotNull(sourceMovement.get("sourceLocationId")),
                  cb.not(cb.exists(sourceLocation))));

          var inaccessibleDestination = query.subquery(Long.class);
          var destinationMovement = inaccessibleDestination.from(InventoryMovementJpaEntity.class);
          var destinationLocation = query.subquery(Long.class);
          var destination = destinationLocation.from(StorageLocationJpaEntity.class);
          destinationLocation.select(destination.get("id"));
          destinationLocation.where(
              cb.and(
                  cb.equal(destination.get("id"), destinationMovement.get("destinationLocationId")),
                  cb.isNull(destination.get("deletedAt")),
                  destination.get("headquarterId").in(criteria.headquarterIds())));
          inaccessibleDestination.select(destinationMovement.get("id"));
          inaccessibleDestination.where(
              cb.and(
                  cb.equal(destinationMovement.get("transactionId"), root.get("id")),
                  cb.isNotNull(destinationMovement.get("destinationLocationId")),
                  cb.not(cb.exists(destinationLocation))));

          parts.add(cb.exists(movementExists));
          parts.add(cb.not(cb.exists(inaccessibleSource)));
          parts.add(cb.not(cb.exists(inaccessibleDestination)));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
