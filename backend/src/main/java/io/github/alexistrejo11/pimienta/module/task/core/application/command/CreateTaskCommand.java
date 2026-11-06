package io.github.alexistrejo11.pimienta.module.task.core.application.command;

import io.github.alexistrejo11.pimienta.module.task.core.domain.ChecklistDraft;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import java.time.LocalDateTime;
import java.util.List;

/** Input for creating a task (application / use cases). Format validation lives on web DTOs. */
public record CreateTaskCommand(
    String title,
    String description,
    Task.Priority priority,
    LocalDateTime dueDate,
    Long headquarterId,
    Long projectId,
    Long opportunityId,
    Long createdById,
    Task.Status initialStatus,
    List<ChecklistDraft> checklistLines) {

  public CreateTaskCommand {
    priority = priority != null ? priority : Task.Priority.MEDIUM;
    initialStatus = initialStatus != null ? initialStatus : Task.Status.PENDING;
    checklistLines = checklistLines != null ? checklistLines : List.of();
  }
}
