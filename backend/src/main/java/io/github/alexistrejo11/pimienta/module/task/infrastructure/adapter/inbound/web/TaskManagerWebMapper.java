package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.task.core.application.command.CreateTaskCommand;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.domain.ChecklistDraft;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.ChecklistItemResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.ChecklistLineRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskListItemResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskSearchRequest;
import java.util.ArrayList;
import java.util.List;

public final class TaskManagerWebMapper {

  private TaskManagerWebMapper() {}

  public static CreateTaskCommand toCreateCommandForOpportunity(
      TaskRequest request, Long opportunityId) {
    return buildCreateCommand(request, opportunityId);
  }

  public static TaskSearchCriteria toCriteria(TaskSearchRequest request) {
    return new TaskSearchCriteria(
        request.getHeadquarterId(),
        request.getProjectId(),
        request.getOpportunityId(),
        request.getEmployeeId(),
        request.getStatus());
  }

  static CreateTaskCommand toCreateCommand(TaskRequest request) {
    return buildCreateCommand(request, request.opportunityId());
  }

  private static CreateTaskCommand buildCreateCommand(TaskRequest request, Long opportunityId) {
    List<ChecklistDraft> drafts = new ArrayList<>();
    if (request.checklist() != null) {
      for (ChecklistLineRequest line : request.checklist()) {
        drafts.add(new ChecklistDraft(line.description(), line.displayOrder()));
      }
    }
    return new CreateTaskCommand(
        request.title(),
        request.description(),
        request.priority(),
        request.dueDate(),
        request.headquarterId(),
        request.projectId(),
        opportunityId,
        request.createdById(),
        Task.Status.PENDING,
        drafts);
  }

  public static TaskResponse toResponse(Task task) {
    List<ChecklistItemResponse> checklist = new ArrayList<>();
    for (Task.ChecklistItem item : task.getChecklist()) {
      checklist.add(
          new ChecklistItemResponse(
              item.description(),
              item.isCompleted(),
              item.completedAt(),
              item.displayOrder()));
    }
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getPriority(),
        task.getAssignedToId(),
        task.getAssignedById(),
        task.getCreatedById(),
        task.getAssignedAt(),
        task.getCompletedAt(),
        task.getDueDate(),
        task.getHeadquarterId(),
        task.getProjectId(),
        task.getOpportunityId(),
        checklist,
        task.getProgressPercentage(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }

  public static TaskListItemResponse toListItem(Task task) {
    return new TaskListItemResponse(
        task.getId(),
        task.getTitle(),
        task.getStatus(),
        task.getPriority(),
        task.getAssignedToId(),
        task.getDueDate(),
        task.getHeadquarterId(),
        task.getProjectId(),
        task.getOpportunityId(),
        task.getProgressPercentage());
  }
}
