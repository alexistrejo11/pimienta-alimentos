package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(
    name = "TaskResponse",
    description =
        """
        Task detail for API responses. Status/priority may show **UNDEFINED** when missing in \
        storage.""")
public record TaskResponse(
    @Schema(description = "Persisted id.", example = "101")
    Long id,
    @Schema(description = "Title.", example = "Call supplier")
    String title,
    @Schema(description = "Description.", example = "Confirm delivery slot.")
    String description,
    @Schema(description = "Workflow status.", example = "IN_PROGRESS")
    Task.Status status,
    @Schema(description = "Priority.", example = "HIGH")
    Task.Priority priority,
    @Schema(description = "Assignee employee id.", example = "7")
    Long assignedToId,
    @Schema(description = "Assigning user id.", example = "3")
    Long assignedById,
    @Schema(description = "Creator user id.", example = "3")
    Long createdById,
    @Schema(type = "string", format = "date-time")
    LocalDateTime assignedAt,
    @Schema(type = "string", format = "date-time")
    LocalDateTime completedAt,
    @Schema(type = "string", format = "date-time")
    LocalDateTime dueDate,
    @Schema(example = "1")
    Long headquarterId,
    @Schema(description = "Checklist lines.")
    List<ChecklistItemResponse> checklist,
    @Schema(description = "Completion percentage from checklist.", example = "50.0")
    double progressPercent,
    @Schema(type = "string", format = "date-time")
    LocalDateTime createdAt,
    @Schema(type = "string", format = "date-time")
    LocalDateTime updatedAt) {}
