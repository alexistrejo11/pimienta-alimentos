package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Schema(
    name = "TaskRequest",
    description =
        """
        Create-task payload. **title** is required. **priority** defaults to MEDIUM in the API \
        layer when omitted. Format rules use Jakarta Validation.""")
public record TaskRequest(
    @NotBlank
        @Schema(description = "Task title.", example = "Follow up with supplier", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,
    @Schema(description = "Longer description (optional).", example = "Call before 17:00.")
    String description,
    @Schema(
        description = "Priority; omit for MEDIUM.",
        example = "HIGH")
    Task.Priority priority,
    @Schema(description = "Optional due date-time.", type = "string", format = "date-time")
    LocalDateTime dueDate,
    @Schema(description = "Headquarter scope (optional).", example = "1")
    Long headquarterId,
    @Schema(description = "Actor who created the task (optional).", example = "42")
    Long createdById,
    @Valid @Schema(description = "Optional checklist lines.")
    List<ChecklistLineRequest> checklist) {}
