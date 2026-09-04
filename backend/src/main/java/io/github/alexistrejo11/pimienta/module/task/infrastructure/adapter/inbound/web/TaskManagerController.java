package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.task.core.application.TaskBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.task.core.application.TaskManagementUseCases;
import io.github.alexistrejo11.pimienta.module.task.core.application.dto.TaskBulkImportResult;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.application.command.CreateTaskCommand;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskAssign;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskCreate;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskDelete;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskExport;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskGetById;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskImport;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskSearch;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTasks;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskToggleChecklistItem;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc.DocTaskUpdateStatus;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.AssignTaskRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.StatusUpdateRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskBulkImportResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskListItemResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskSearchRequest;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/tasks")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocTasks
public class TaskManagerController {

  private final TaskManagementUseCases taskManagementUseCases;
  private final TaskBulkSyncUseCases taskBulkSyncUseCases;

  public TaskManagerController(
      TaskManagementUseCases taskManagementUseCases,
      TaskBulkSyncUseCases taskBulkSyncUseCases) {
    this.taskManagementUseCases = taskManagementUseCases;
    this.taskBulkSyncUseCases = taskBulkSyncUseCases;
  }

  /** Búsqueda global con filtros opcionales (sede, proyecto, empleado, estado). */
  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocTaskSearch
  public PagedResponse<TaskListItemResponse> searchTasks(@ModelAttribute TaskSearchRequest filter) {
    TaskSearchCriteria criteria = TaskManagerWebMapper.toCriteria(filter);
    Page<Task> page =
        taskManagementUseCases.search(
            criteria, PageRequest.of(filter.getPage(), filter.getSize()));
    return PagedResponse.map(page, TaskManagerWebMapper::toListItem);
  }

  /**
   * Exporta tareas a Excel (mismos filtros opcionales que {@link #searchTasks}). La paginación del
   * listado no aplica: se exportan todas las filas que cumplan el criterio.
   */
  @GetMapping("/export")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocTaskExport
  public ResponseEntity<byte[]> exportTasksToExcel(
      @ModelAttribute TaskSearchRequest filter, Pageable pageable) throws IOException {
    TaskSearchCriteria criteria = TaskManagerWebMapper.toCriteria(filter);
    byte[] excelContent = taskBulkSyncUseCases.exportTasks(criteria, pageable);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tareas_reporte.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excelContent);
  }

  /** Importación masiva (upsert por columna ID). */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocTaskImport
  public TaskBulkImportResponse importTasksFromExcel(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun)
      throws IOException {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    TaskBulkImportResult result =
        taskBulkSyncUseCases.importTasks(file.getInputStream(), file.getOriginalFilename(), dryRun);
    return TaskBulkImportResponse.from(result);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocTaskGetById
  public TaskResponse getTaskById(@PathVariable Long id) {
    Task task = taskManagementUseCases.getById(id);
    return TaskManagerWebMapper.toResponse(task);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocTaskCreate
  public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
    CreateTaskCommand command = TaskManagerWebMapper.toCreateCommand(request);
    Task created = taskManagementUseCases.create(command);
    return TaskManagerWebMapper.toResponse(created);
  }

  @PatchMapping("/{id}/status")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocTaskUpdateStatus
  public TaskResponse updateStatus(
      @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest status) {
    Task updated = taskManagementUseCases.updateStatus(id, status.status());
    return TaskManagerWebMapper.toResponse(updated);
  }

  @PatchMapping("/{id}/assign")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocTaskAssign
  public TaskResponse assignTask(@PathVariable Long id, @Valid @RequestBody AssignTaskRequest request) {
    Task updated = taskManagementUseCases.assign(id, request.employeeId());
    return TaskManagerWebMapper.toResponse(updated);
  }

  @PatchMapping("/{id}/checklist/{itemOrder}/toggle")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocTaskToggleChecklistItem
  public TaskResponse toggleChecklistItem(@PathVariable Long id, @PathVariable int itemOrder) {
    Task updated = taskManagementUseCases.toggleChecklistItem(id, itemOrder);
    return TaskManagerWebMapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocTaskDelete
  public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
    taskManagementUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }
}
