package io.github.alexistrejo11.pimienta.module.task.core.application.query;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;

/** Filtros opcionales para listar tareas (sede, empleado, estado). */
public record TaskSearchCriteria(
    Long headquarterId,
    Long assignedToId,
    Task.Status status) {

  public static TaskSearchCriteria empty() {
    return new TaskSearchCriteria(null, null, null);
  }
}
