package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateProjectMilestoneParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.UpdateProjectMilestoneParams;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.ProjectMilestone;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.ProjectMilestone.MilestoneStatus;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.exception.ProjectMilestoneNotFoundException;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.exception.ProjectNotFoundException;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.ProjectMilestoneUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ProjectMilestoneRepository;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.ProjectRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProjectMilestoneUseCasesImpl implements ProjectMilestoneUseCases {

  private static final Logger log = LoggerFactory.getLogger(ProjectMilestoneUseCasesImpl.class);

  private final ProjectMilestoneRepository milestoneRepository;
  private final ProjectRepository projectRepository;

  public ProjectMilestoneUseCasesImpl(
      ProjectMilestoneRepository milestoneRepository, ProjectRepository projectRepository) {
    this.milestoneRepository = milestoneRepository;
    this.projectRepository = projectRepository;
  }

  @Override
  public Page<ProjectMilestone> listByProject(Long projectId, Pageable pageable) {
    log.debug(
        "list milestones by project query start projectId={} page={} size={}",
        projectId,
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null);

    ensureProjectExists(projectId);

    Page<ProjectMilestone> page = milestoneRepository.findByProjectId(projectId, pageable);

    log.debug(
        "list milestones by project query complete projectId={} totalElements={} numberOfElements={}",
        projectId,
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public ProjectMilestone getById(Long projectId, Long milestoneId) {
    log.debug(
        "get milestone by id query start projectId={} milestoneId={}",
        projectId,
        milestoneId);

    ensureProjectExists(projectId);

    ProjectMilestone m =
        milestoneRepository
            .findByIdAndProjectId(milestoneId, projectId)
            .orElseThrow(() -> new ProjectMilestoneNotFoundException(projectId, milestoneId));

    log.debug(
        "get milestone by id query complete projectId={} milestoneId={}",
        projectId,
        m.getId());
    return m;
  }

  @Override
  public ProjectMilestone create(Long projectId, CreateProjectMilestoneParams params) {
    log.info(
        "create milestone start projectId={} plannedDate={} sortOrder={}",
        projectId,
        params.plannedDate(),
        params.sortOrder());

    ensureProjectExists(projectId);

    ProjectMilestone saved =
        milestoneRepository.save(
            ProjectMilestone.builder()
                .withProjectId(projectId)
                .withName(params.name())
                .withDescription(params.description())
                .withPlannedDate(params.plannedDate())
                .withBillingAmount(params.billingAmount())
                .withSortOrder(params.sortOrder())
                .register());

    log.info(
        "create milestone complete projectId={} milestoneId={}",
        projectId,
        saved.getId());
    return saved;
  }

  @Override
  public ProjectMilestone update(
      Long projectId, Long milestoneId, UpdateProjectMilestoneParams params) {
    log.info("update milestone start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);

    if (params.name() != null) {
      m.setName(params.name());
    }
    if (params.description() != null) {
      m.setDescription(params.description());
    }
    if (params.plannedDate() != null) {
      m.setPlannedDate(params.plannedDate());
    }
    if (params.billingAmount() != null) {
      m.setBillingAmount(params.billingAmount());
    }
    if (params.sortOrder() != null) {
      m.setSortOrder(params.sortOrder());
    }
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("update milestone complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  @Override
  public void delete(Long projectId, Long milestoneId) {
    log.info("delete milestone start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    m.softDelete();

    milestoneRepository.save(m);
    log.info("delete milestone complete projectId={} milestoneId={}", projectId, milestoneId);
  }

  @Override
  public ProjectMilestone start(Long projectId, Long milestoneId) {
    log.info("milestone start start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    m.setStatus(MilestoneStatus.IN_PROGRESS);
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("milestone start complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  @Override
  public ProjectMilestone complete(Long projectId, Long milestoneId) {
    log.info("milestone markCompleted start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    m.setStatus(MilestoneStatus.COMPLETED);
    m.setActualDate(LocalDate.now(ClientTimeZones.current()));
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("milestone markCompleted complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  @Override
  public ProjectMilestone markDelayed(Long projectId, Long milestoneId) {
    log.info("milestone markDelayed start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    m.setStatus(MilestoneStatus.DELAYED);
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("milestone markDelayed complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  @Override
  public ProjectMilestone cancel(Long projectId, Long milestoneId) {
    log.info("milestone cancel start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    m.setStatus(MilestoneStatus.CANCELLED);
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("milestone cancel complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  @Override
  public ProjectMilestone markBilled(Long projectId, Long milestoneId) {
    log.info("milestone markBilled start projectId={} milestoneId={}", projectId, milestoneId);

    ProjectMilestone m = getById(projectId, milestoneId);
    if (m.getStatus() != MilestoneStatus.COMPLETED) {
      throw new IllegalStateException("Only COMPLETED milestones can be marked billed");
    }
    m.setBilled(true);
    m.touch();

    ProjectMilestone saved = milestoneRepository.save(m);
    log.info("milestone markBilled complete projectId={} milestoneId={}", projectId, saved.getId());
    return saved;
  }

  private void ensureProjectExists(Long projectId) {
    projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
  }
}
