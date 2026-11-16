package io.github.alexistrejo11.pimienta.module.task.core.application;

import io.github.alexistrejo11.pimienta.module.task.core.application.command.CreateTaskCommand;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.module.task.core.domain.exception.TaskChecklistItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.task.core.domain.exception.TaskNotFoundException;
import io.github.alexistrejo11.pimienta.module.task.core.port.TaskRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskManagementUseCasesImpl implements TaskManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(TaskManagementUseCasesImpl.class);

  private final TaskRepository taskRepository;

  public TaskManagementUseCasesImpl(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Override
  public Page<Task> search(TaskSearchCriteria criteria, Pageable pageable) {
    TaskSearchCriteria effective = criteria != null ? criteria : TaskSearchCriteria.empty();

    log.debug(
        "search tasks query start page={} size={} headquarterId={} projectId={} opportunityId={} assignedToId={} status={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.headquarterId(),
        effective.projectId(),
        effective.opportunityId(),
        effective.assignedToId(),
        effective.status());

    Page<Task> page = taskRepository.search(effective, pageable);

    log.debug(
        "search tasks query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Task getById(Long id) {
    log.debug("get task by id query start taskId={}", id);

    Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

    log.debug("get task by id query complete taskId={}", task.getId());
    return task;
  }

  @Override
  public Task create(CreateTaskCommand command) {
    int titleLen = command.title() != null ? command.title().length() : 0;
    int checklistCount = command.checklistLines() != null ? command.checklistLines().size() : 0;

    log.info(
        "create task start titleLen={} priority={} initialStatus={} headquarterId={} projectId={} opportunityId={} createdById={} checklistLineCount={}",
        titleLen,
        command.priority(),
        command.initialStatus(),
        command.headquarterId(),
        command.projectId(),
        command.opportunityId(),
        command.createdById(),
        checklistCount);

    Task created =
        Task.builder()
            .withTitle(command.title())
            .withDescription(command.description() != null ? command.description() : "")
            .withPriority(command.priority())
            .withDueDate(command.dueDate())
            .withHeadquarterId(command.headquarterId())
            .withProjectId(command.projectId())
            .withOpportunityId(command.opportunityId())
            .withCreatedById(command.createdById())
            .withStatus(command.initialStatus())
            .withChecklistDrafts(command.checklistLines())
            .register();

    Task saved = taskRepository.save(created);

    log.info("create task complete taskId={}", saved.getId());
    return saved;
  }

  @Override
  public Task updateStatus(Long id, Task.Status newStatus) {
    log.info("update task status start taskId={} newStatus={}", id, newStatus);

    Task task = getById(id);
    LocalDateTime when = LocalDateTime.now();
    task.setStatus(newStatus);
    if (newStatus == Task.Status.COMPLETED) {
      task.setCompletedAt(when);
    } else {
      task.setCompletedAt(null);
    }
    task.touch();

    Task saved = taskRepository.save(task);
    log.info("update task status complete taskId={}", saved.getId());
    return saved;
  }

  @Override
  public Task assign(Long id, Long employeeId) {
    log.info("assign task start taskId={} employeeId={}", id, employeeId);

    Task task = getById(id);
    task.setAssignedToId(employeeId);
    task.setAssignedAt(LocalDateTime.now());
    task.touch();

    Task saved = taskRepository.save(task);
    log.info("assign task complete taskId={}", saved.getId());
    return saved;
  }

  @Override
  public Task toggleChecklistItem(Long id, int itemOrder) {
    log.info("toggle task checklist item start taskId={} itemOrder={}", id, itemOrder);

    Task task = getById(id);
    List<Task.ChecklistItem> current = new ArrayList<>(task.getChecklist());
    boolean found = false;
    List<Task.ChecklistItem> next = new ArrayList<>();
    for (Task.ChecklistItem it : current) {
      if (it.displayOrder() == itemOrder) {
        found = true;
        boolean done = !it.isCompleted();
        next.add(
            new Task.ChecklistItem(
                it.description(),
                done,
                done ? LocalDateTime.now() : null,
                it.displayOrder()));
      } else {
        next.add(it);
      }
    }
    if (!found) {
      throw new TaskChecklistItemNotFoundException(id, itemOrder);
    }
    task.setChecklist(next);
    task.touch();

    Task saved = taskRepository.save(task);
    log.info("toggle task checklist item complete taskId={}", saved.getId());
    return saved;
  }

  @Override
  public void delete(Long id) {
    log.info("delete task start taskId={}", id);

    Task task = getById(id);
    task.delete();

    taskRepository.save(task);
    log.info("delete task complete taskId={}", id);
  }
}
