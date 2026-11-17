package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class TaskSpecifications {

  private static final List<Task.Status> CLOSED_STATUSES =
      List.of(Task.Status.COMPLETED, Task.Status.CANCELLED);

  private TaskSpecifications() {}

  static Specification<TaskJpaEntity> notDeleted() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  /** Excluye COMPLETED y CANCELLED. */
  static Specification<TaskJpaEntity> statusOpen() {
    return (root, query, cb) -> cb.not(root.get("status").in(CLOSED_STATUSES));
  }

  static Specification<TaskJpaEntity> statusPending() {
    return (root, query, cb) -> cb.equal(root.get("status"), Task.Status.PENDING);
  }

  static Specification<TaskJpaEntity> fromCriteria(TaskSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.headquarterId() != null) {
          parts.add(cb.equal(root.get("headquarterId"), criteria.headquarterId()));
        }
        if (criteria.assignedToId() != null) {
          parts.add(cb.equal(root.get("assignedToId"), criteria.assignedToId()));
        }
        if (criteria.status() != null) {
          parts.add(cb.equal(root.get("status"), criteria.status()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
