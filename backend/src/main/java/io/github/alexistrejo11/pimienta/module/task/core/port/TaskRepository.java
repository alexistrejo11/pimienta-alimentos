package io.github.alexistrejo11.pimienta.module.task.core.port;

import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskRepository {

  Optional<Task> findById(long id);

  Page<Task> search(TaskSearchCriteria criteria, Pageable pageable);

  Task save(Task task);

  /** Tareas no borradas que no están COMPLETED ni CANCELLED. */
  long countOpenTasks();

  /** Tareas no borradas en estado PENDING. */
  long countPendingTasks();
}
