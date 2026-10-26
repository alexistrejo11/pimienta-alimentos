package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateProjectParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.UpdateProjectParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.ProjectSearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.application.summary.ProjectSummary;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project.ProjectStatus;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.exception.ProjectNotFoundException;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.ProjectUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ProjectMilestoneRepository;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ProjectRepository;
import io.github.alexistrejo11.pimienta.module.task.core.port.TaskRepository;
import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProjectUseCasesImpl implements ProjectUseCases {

  private static final Logger log = LoggerFactory.getLogger(ProjectUseCasesImpl.class);

  private final ProjectRepository projectRepository;
  private final ProjectMilestoneRepository milestoneRepository;
  private final TaskRepository taskRepository;

  public ProjectUseCasesImpl(
      ProjectRepository projectRepository,
      ProjectMilestoneRepository milestoneRepository,
      TaskRepository taskRepository) {
    this.projectRepository = projectRepository;
    this.milestoneRepository = milestoneRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  public Page<Project> search(ProjectSearchCriteria criteria, Pageable pageable) {
    ProjectSearchCriteria effective = criteria != null ? criteria : ProjectSearchCriteria.empty();

    log.debug(
        "search projects query start page={} size={} status={} clientId={} projectManagerId={} originOpportunityId={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.status(),
        effective.clientId(),
        effective.projectManagerId(),
        effective.originOpportunityId());

    Page<Project> page = projectRepository.search(effective, pageable);

    log.debug(
        "search projects query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Project getById(Long id) {
    log.debug("get project by id query start projectId={}", id);

    Project p = projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));

    log.debug("get project by id query complete projectId={}", p.getId());
    return p;
  }

  @Override
  public Project create(CreateProjectParams params) {
    String code = params.projectCode().trim();
    log.info(
        "create project start projectCode={} clientId={} type={} originOpportunityId={}",
        code,
        params.clientId(),
        params.type(),
        params.originOpportunityId());

    if (projectRepository.findByProjectCode(code).isPresent()) {
      throw new ConflictException(
          ErrorCode.CONFLICT,
          "A project with this code already exists.",
          Map.of("projectCode", code),
          "Duplicate projectCode: " + code);
    }

    Project saved =
        projectRepository.save(
            Project.builder()
                .withProjectCode(code)
                .withProjectName(params.projectName())
                .withDescription(params.description())
                .withClientId(params.clientId())
                .withOriginOpportunityId(params.originOpportunityId())
                .withType(params.type())
                .withPriority(params.priority())
                .withProjectManagerId(params.projectManagerId())
                .withAssignedSalesmanId(params.assignedSalesmanId())
                .withPlannedStartDate(params.plannedStartDate())
                .withPlannedEndDate(params.plannedEndDate())
                .withContractedValue(params.contractedValue())
                .withEstimatedCost(params.estimatedCost())
                .register());

    log.info("create project complete projectId={} projectCode={}", saved.getId(), code);
    return saved;
  }

  @Override
  public Project update(Long id, UpdateProjectParams params) {
    log.info("update project start projectId={}", id);

    Project p = getById(id);

    if (params.projectName() != null) {
      p.setProjectName(params.projectName());
    }
    if (params.description() != null) {
      p.setDescription(params.description());
    }
    if (params.type() != null) {
      p.setType(params.type());
    }
    if (params.priority() != null) {
      p.setPriority(params.priority());
    }
    if (params.projectManagerId() != null) {
      p.setProjectManagerId(params.projectManagerId());
    }
    if (params.assignedSalesmanId() != null) {
      p.setAssignedSalesmanId(params.assignedSalesmanId());
    }
    if (params.plannedStartDate() != null) {
      p.setPlannedStartDate(params.plannedStartDate());
    }
    if (params.plannedEndDate() != null) {
      p.setPlannedEndDate(params.plannedEndDate());
    }
    if (params.contractedValue() != null) {
      p.setContractedValue(params.contractedValue());
    }
    if (params.estimatedCost() != null) {
      p.setEstimatedCost(params.estimatedCost());
    }
    if (params.progressPercent() != null) {
      int percent = params.progressPercent();
      if (percent < 0 || percent > 100) {
        throw new IllegalArgumentException("Progress must be between 0 and 100");
      }
      p.setProgressPercent(percent);
    }
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("update project complete projectId={}", saved.getId());
    return saved;
  }

  @Override
  public void delete(Long id) {
    log.info("delete project start projectId={}", id);

    Project p = getById(id);
    p.softDelete();

    projectRepository.save(p);
    log.info("delete project complete projectId={}", id);
  }

  @Override
  public ProjectSummary getSummary(Long id) {
    log.debug("get project summary query start projectId={}", id);

    Project p = getById(id);
    long milestoneCount = milestoneRepository.countByProjectId(id);
    long milestoneCompleted = milestoneRepository.countCompletedByProjectId(id);
    long taskCount = taskRepository.countByProjectId(id);

    ProjectSummary summary =
        new ProjectSummary(p, milestoneCount, milestoneCompleted, taskCount, p.isOverdue());

    log.debug(
        "get project summary query complete projectId={} milestoneCount={} milestoneCompleted={} taskCount={}",
        id,
        milestoneCount,
        milestoneCompleted,
        taskCount);
    return summary;
  }

  @Override
  public Project activate(Long id) {
    log.info("project activate start projectId={}", id);

    Project p = getById(id);
    if (p.getStatus() != ProjectStatus.PLANNING && p.getStatus() != ProjectStatus.ON_HOLD) {
      throw new IllegalStateException("Cannot activate from status " + p.getStatus());
    }
    p.setStatus(ProjectStatus.ACTIVE);
    if (p.getActualStartDate() == null) {
      p.setActualStartDate(LocalDate.now(ClientTimeZones.current()));
    }
    p.setOnHoldReason(null);
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("project activate complete projectId={}", saved.getId());
    return saved;
  }

  @Override
  public Project putOnHold(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Hold reason is required");
    }
    log.info("project putOnHold start projectId={} reasonLen={}", id, reason.length());

    Project p = getById(id);
    if (p.getStatus() != ProjectStatus.ACTIVE) {
      throw new IllegalStateException("Only ACTIVE projects can be put on hold");
    }
    p.setStatus(ProjectStatus.ON_HOLD);
    p.setOnHoldReason(reason);
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("project putOnHold complete projectId={}", saved.getId());
    return saved;
  }

  @Override
  public Project complete(Long id) {
    log.info("project markCompleted start projectId={}", id);

    Project p = getById(id);
    if (p.getStatus() != ProjectStatus.ACTIVE) {
      throw new IllegalStateException("Only ACTIVE projects can be completed");
    }
    p.setStatus(ProjectStatus.COMPLETED);
    p.setProgressPercent(100);
    p.setActualEndDate(LocalDate.now(ClientTimeZones.current()));
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("project markCompleted complete projectId={}", saved.getId());
    return saved;
  }

  @Override
  public Project cancel(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Cancellation reason is required");
    }
    log.info("project cancel start projectId={} reasonLen={}", id, reason.length());

    Project p = getById(id);
    if (p.getStatus() == ProjectStatus.COMPLETED || p.getStatus() == ProjectStatus.ARCHIVED) {
      throw new IllegalStateException("Cannot cancel project in status " + p.getStatus());
    }
    p.setStatus(ProjectStatus.CANCELLED);
    p.setCancellationReason(reason);
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("project cancel complete projectId={}", saved.getId());
    return saved;
  }

  @Override
  public Project archive(Long id) {
    log.info("project archive start projectId={}", id);

    Project p = getById(id);
    if (p.getStatus() != ProjectStatus.COMPLETED && p.getStatus() != ProjectStatus.CANCELLED) {
      throw new IllegalStateException("Only COMPLETED or CANCELLED projects can be archived");
    }
    p.setStatus(ProjectStatus.ARCHIVED);
    p.touch();

    Project saved = projectRepository.save(p);
    log.info("project archive complete projectId={}", saved.getId());
    return saved;
  }
}
