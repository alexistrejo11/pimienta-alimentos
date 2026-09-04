package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.task.core.application.dto.TaskBulkImportResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** HTTP JSON response for bulk task import. */
@Schema(
    name = "TaskBulkImportResponse",
    description = "Counters for updated/created/skipped rows plus per-row errors.")
public record TaskBulkImportResponse(
    @Schema(example = "3")
    int updated,
    @Schema(example = "5")
    int created,
    @Schema(example = "1")
    int skipped,
    @Schema(description = "Row-level errors.")
    List<TaskBulkImportRowResponse> errors,
    @Schema(description = "true if the file was validated without persisting.")
    boolean dryRun) {

  public static TaskBulkImportResponse from(TaskBulkImportResult result) {
    List<TaskBulkImportRowResponse> err =
        result.errors().stream()
            .map(e -> new TaskBulkImportRowResponse(e.excelRowNumber(), e.message()))
            .toList();
    return new TaskBulkImportResponse(
        result.updated(), result.created(), result.skipped(), err, result.dryRun());
  }
}
