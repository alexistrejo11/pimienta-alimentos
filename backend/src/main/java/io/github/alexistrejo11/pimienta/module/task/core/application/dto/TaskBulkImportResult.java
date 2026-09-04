package io.github.alexistrejo11.pimienta.module.task.core.application.dto;

import java.util.List;

public record TaskBulkImportResult(
    int updated,
    int created,
    int skipped,
    List<TaskBulkImportRowError> errors,
    boolean dryRun) {}
