package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectActivate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectArchive;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectCancel;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectComplete;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectCreate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectDelete;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectGetById;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectGetSummary;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectHold;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectListTasks;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectSearch;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjectUpdate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocProjects;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.ProjectUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ProjectSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.CreateProjectRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ProjectResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ProjectSearchRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ProjectSummaryResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ReasonRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.UpdateProjectRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.mapper.ProjectWebMapper;
import io.github.alexistrejo11.pimienta.module.task.core.application.TaskManagementUseCases;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.TaskManagerWebMapper;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskListItemResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskSearchRequest;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/projects")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocProjects
public class ProjectController {

  private final ProjectUseCases projectUseCases;
  private final TaskManagementUseCases taskManagementUseCases;

  public ProjectController(
      ProjectUseCases projectUseCases, TaskManagementUseCases taskManagementUseCases) {
    this.projectUseCases = projectUseCases;
    this.taskManagementUseCases = taskManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProjectSearch
  public PagedResponse<ProjectResponse> searchProjects(@ModelAttribute ProjectSearchRequest filter) {
    ProjectSearchCriteria criteria = ProjectWebMapper.toCriteria(filter);
    Page<Project> page =
        projectUseCases.search(criteria, PageRequest.of(filter.getPage(), filter.getSize()));
    return PagedResponse.map(page, ProjectWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProjectGetById
  public ProjectResponse getProject(@PathVariable Long id) {
    Project project = projectUseCases.getById(id);
    return ProjectWebMapper.toResponse(project);
  }

  @GetMapping("/{id}/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProjectGetSummary
  public ProjectSummaryResponse getProjectSummary(@PathVariable Long id) {
    return ProjectWebMapper.toSummaryResponse(projectUseCases.getSummary(id));
  }

  @GetMapping("/{id}/tasks")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProjectListTasks
  public PagedResponse<TaskListItemResponse> listProjectTasks(
      @PathVariable Long id, @ModelAttribute TaskSearchRequest filter) {
    projectUseCases.getById(id);
    filter.setProjectId(id);
    TaskSearchCriteria criteria = TaskManagerWebMapper.toCriteria(filter);
    Page<Task> page =
        taskManagementUseCases.search(
            criteria, PageRequest.of(filter.getPage(), filter.getSize()));
    return PagedResponse.map(page, TaskManagerWebMapper::toListItem);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocProjectCreate
  public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
    Project created = projectUseCases.create(ProjectWebMapper.toCreateParams(request));
    return ProjectWebMapper.toResponse(created);
  }

  @PatchMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectUpdate
  public ProjectResponse updateProject(
      @PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
    Project updated = projectUseCases.update(id, ProjectWebMapper.toUpdateParams(request));
    return ProjectWebMapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectDelete
  public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
    projectUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/activate")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectActivate
  public ProjectResponse activate(@PathVariable Long id) {
    Project p = projectUseCases.activate(id);
    return ProjectWebMapper.toResponse(p);
  }

  @PostMapping("/{id}/hold")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectHold
  public ProjectResponse putOnHold(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    Project p = projectUseCases.putOnHold(id, request.reason());
    return ProjectWebMapper.toResponse(p);
  }

  @PostMapping("/{id}/complete")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectComplete
  public ProjectResponse complete(@PathVariable Long id) {
    Project p = projectUseCases.complete(id);
    return ProjectWebMapper.toResponse(p);
  }

  @PostMapping("/{id}/cancel")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectCancel
  public ProjectResponse cancel(
      @PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    Project p = projectUseCases.cancel(id, request.reason());
    return ProjectWebMapper.toResponse(p);
  }

  @PostMapping("/{id}/archive")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProjectArchive
  public ProjectResponse archive(@PathVariable Long id) {
    Project p = projectUseCases.archive(id);
    return ProjectWebMapper.toResponse(p);
  }
}
