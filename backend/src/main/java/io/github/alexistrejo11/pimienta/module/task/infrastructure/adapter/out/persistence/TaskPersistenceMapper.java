package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import java.util.ArrayList;
import java.util.List;

final class TaskPersistenceMapper {

  private TaskPersistenceMapper() {}

  static TaskJpaEntity toJpa(Task domain) {
    TaskJpaEntity e = new TaskJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setTitle(blankToNull(domain.getTitle()));
    e.setDescription(blankToNull(domain.getDescription()));
    e.setStatus(domain.getStatusOrNull());
    e.setPriority(domain.getPriorityOrNull());
    e.setAssignedToId(domain.getAssignedToId());
    e.setAssignedById(domain.getAssignedById());
    e.setCreatedById(domain.getCreatedById());
    e.setAssignedAt(domain.getAssignedAt());
    e.setCompletedAt(domain.getCompletedAt());
    e.setDueDate(domain.getDueDate());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setProjectId(domain.getProjectId());
    e.setOpportunityId(domain.getOpportunityId());
    List<ChecklistItemJson> jsonItems = new ArrayList<>();
    for (Task.ChecklistItem item : domain.getChecklist()) {
      jsonItems.add(
          new ChecklistItemJson(
              item.description(),
              item.isCompleted(),
              item.completedAt(),
              item.displayOrder()));
    }
    e.setChecklist(jsonItems);
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  static Task toDomain(TaskJpaEntity e) {
    List<Task.ChecklistItem> items = new ArrayList<>();
    if (e.getChecklist() != null) {
      for (ChecklistItemJson j : e.getChecklist()) {
        items.add(
            new Task.ChecklistItem(
                j.description() != null ? j.description() : "",
                j.isCompleted(),
                j.completedAt(),
                j.displayOrder()));
      }
    }
    return Task.builder()
        .withId(e.getId())
        .withTitle(text(e.getTitle()))
        .withDescription(text(e.getDescription()))
        .withStatus(e.getStatus())
        .withPriority(e.getPriority())
        .withAssignedToId(e.getAssignedToId())
        .withAssignedById(e.getAssignedById())
        .withCreatedById(e.getCreatedById())
        .withAssignedAt(e.getAssignedAt())
        .withCompletedAt(e.getCompletedAt())
        .withDueDate(e.getDueDate())
        .withHeadquarterId(e.getHeadquarterId())
        .withProjectId(e.getProjectId())
        .withOpportunityId(e.getOpportunityId())
        .withChecklistItems(items)
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .reconstruct();
  }

  private static String text(String s) {
    return s != null ? s : "";
  }

  private static String blankToNull(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.strip();
  }

  private static java.time.LocalDateTime now() {
    return java.time.LocalDateTime.now();
  }
}
