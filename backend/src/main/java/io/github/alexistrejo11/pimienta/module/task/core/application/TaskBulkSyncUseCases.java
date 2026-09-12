package io.github.alexistrejo11.pimienta.module.task.core.application;

import io.github.alexistrejo11.pimienta.module.task.core.application.dto.TaskBulkImportResult;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;

public interface TaskBulkSyncUseCases {

  byte[] exportTasks(TaskSearchCriteria criteria, Pageable pageable) throws IOException;

  TaskBulkImportResult importTasks(InputStream file, String originalFilename, boolean dryRun)
      throws IOException;
}
