package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "TaskListItemResponse", description = "Summary row for task search results.")
public record TaskListItemResponse(
    @Schema(example = "101")
    Long id,
    @Schema(example = "Call supplier")
    String title,
    Task.Status status,
    Task.Priority priority,
    @Schema(example = "7")
    Long assignedToId,
    @Schema(type = "string", format = "date-time")
    LocalDateTime dueDate,
    @Schema(example = "1")
    Long headquarterId,
    @Schema(example = "33.33")
    double progressPercent) {}
