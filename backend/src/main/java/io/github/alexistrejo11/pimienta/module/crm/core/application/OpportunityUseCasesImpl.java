package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.UpdateOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.WinOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.application.summary.OpportunitySummary;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity.OpportunityStatus;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.exception.OpportunityNotFoundException;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.OpportunityUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.OpportunityRepository;
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
public class OpportunityUseCasesImpl implements OpportunityUseCases {

  private static final Logger log = LoggerFactory.getLogger(OpportunityUseCasesImpl.class);

  private final OpportunityRepository opportunityRepository;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;

  public OpportunityUseCasesImpl(
      OpportunityRepository opportunityRepository,
      ProjectRepository projectRepository,
      TaskRepository taskRepository) {
    this.opportunityRepository = opportunityRepository;
    this.projectRepository = projectRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  public Page<Opportunity> search(OpportunitySearchCriteria criteria, Pageable pageable) {
    OpportunitySearchCriteria effective =
        criteria != null ? criteria : OpportunitySearchCriteria.empty();

    log.debug(
        "search opportunities query start page={} size={} status={} companyNameFilterLen={} titleFilterLen={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.status(),
        effective.companyNameContains() != null ? effective.companyNameContains().length() : 0,
        effective.titleContains() != null ? effective.titleContains().length() : 0);

    Page<Opportunity> page = opportunityRepository.search(effective, pageable);

    log.debug(
        "search opportunities query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public Opportunity getById(Long id) {
    log.debug("get opportunity by id query start opportunityId={}", id);

    Opportunity o =
        opportunityRepository.findById(id).orElseThrow(() -> new OpportunityNotFoundException(id));

    log.debug("get opportunity by id query complete opportunityId={}", o.getId());
    return o;
  }

  @Override
  public Opportunity create(CreateOpportunityParams params) {
    log.info(
        "create opportunity start assignedSalesmanId={} expectedCloseDate={} probabilityPercent={}",
        params.assignedSalesmanId(),
        params.expectedCloseDate(),
        params.probabilityPercent());

    Opportunity saved =
        opportunityRepository.save(
            Opportunity.builder()
                .withTitle(params.title())
                .withDescription(params.description())
                .withContactName(params.contactName())
                .withContactEmail(params.contactEmail())
                .withContactPhone(params.contactPhone())
                .withCompanyName(params.companyName())
                .withCompanyLocation(params.companyLocation())
                .withIndustry(params.industry())
                .withEstimatedValue(params.estimatedValue())
                .withProbabilityPercent(params.probabilityPercent())
                .withSource(params.source())
                .withExpectedCloseDate(params.expectedCloseDate())
                .withAssignedSalesmanId(params.assignedSalesmanId())
                .register());

    log.info("create opportunity complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity update(Long id, UpdateOpportunityParams params) {
    log.info("update opportunity start opportunityId={}", id);

    Opportunity o = getById(id);

    if (params.title() != null) {
      o.setTitle(params.title());
    }
    if (params.description() != null) {
      o.setDescription(params.description());
    }
    if (params.contactName() != null) {
      o.setContactName(params.contactName());
    }
    if (params.contactEmail() != null) {
      o.setContactEmail(params.contactEmail());
    }
    if (params.contactPhone() != null) {
      o.setContactPhone(params.contactPhone());
    }
    if (params.companyName() != null) {
      o.setCompanyName(params.companyName());
    }
    if (params.companyLocation() != null) {
      o.setCompanyLocation(params.companyLocation());
    }
    if (params.industry() != null) {
      o.setIndustry(params.industry());
    }
    if (params.estimatedValue() != null) {
      o.setEstimatedValue(params.estimatedValue());
    }
    if (params.probabilityPercent() != null) {
      int pc = params.probabilityPercent();
      if (pc < 0 || pc > 100) {
        throw new IllegalArgumentException("Probability must be between 0 and 100");
      }
      o.setProbabilityPercent(pc);
    }
    if (params.source() != null) {
      o.setSource(params.source());
    }
    if (params.expectedCloseDate() != null) {
      o.setExpectedCloseDate(params.expectedCloseDate());
    }
    if (params.assignedSalesmanId() != null) {
      o.setAssignedSalesmanId(params.assignedSalesmanId());
    }
    o.touch();

    Opportunity saved = opportunityRepository.save(o);

    log.info("update opportunity complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public void delete(Long id) {
    log.info("delete opportunity start opportunityId={}", id);

    Opportunity o = getById(id);
    o.softDelete();

    opportunityRepository.save(o);
    log.info("delete opportunity complete opportunityId={}", id);
  }

  @Override
  public OpportunitySummary getSummary(Long id) {
    log.debug("get opportunity summary query start opportunityId={}", id);

    Opportunity o = getById(id);
    long taskCount = taskRepository.countByOpportunityId(id);
    long openTaskCount = taskRepository.countOpenByOpportunityId(id);

    OpportunitySummary summary =
        new OpportunitySummary(
            o, taskCount, openTaskCount, o.getWeightedValue(), o.isOverdue());

    log.debug(
        "get opportunity summary query complete opportunityId={} taskCount={} openTaskCount={}",
        id,
        taskCount,
        openTaskCount);
    return summary;
  }

  @Override
  public Opportunity moveToDiscovery(Long id) {
    log.info("opportunity moveToDiscovery start opportunityId={}", id);

    Opportunity o = getById(id);
    if (o.getStatus() != OpportunityStatus.NEW) {
      throw new IllegalStateException("Expected status NEW but was " + o.getStatus());
    }
    o.setStatus(OpportunityStatus.DISCOVERY);
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity moveToDiscovery complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity sendProposal(Long id) {
    log.info("opportunity sendProposal start opportunityId={}", id);

    Opportunity o = getById(id);
    if (o.getStatus() != OpportunityStatus.DISCOVERY) {
      throw new IllegalStateException("Expected status DISCOVERY but was " + o.getStatus());
    }
    o.setStatus(OpportunityStatus.PROPOSAL);
    o.setProbabilityPercent(Math.max(o.getProbabilityPercent(), 40));
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity sendProposal complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity startNegotiation(Long id) {
    log.info("opportunity startNegotiation start opportunityId={}", id);

    Opportunity o = getById(id);
    if (o.getStatus() != OpportunityStatus.PROPOSAL) {
      throw new IllegalStateException("Expected status PROPOSAL but was " + o.getStatus());
    }
    o.setStatus(OpportunityStatus.NEGOTIATION);
    o.setProbabilityPercent(Math.max(o.getProbabilityPercent(), 70));
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity startNegotiation complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity win(Long id, WinOpportunityParams params) {
    String code = params.projectCode().trim();
    log.info(
        "opportunity win start opportunityId={} projectCode={} clientId={} type={}",
        id,
        code,
        params.clientId(),
        params.type());

    Opportunity o = getById(id);
    if (projectRepository.findByProjectCode(code).isPresent()) {
      throw new ConflictException(
          ErrorCode.CONFLICT,
          "A project with this code already exists.",
          Map.of("projectCode", code),
          "Duplicate projectCode on win: " + code);
    }

    Project savedProject =
        projectRepository.save(
            Project.builder()
                .withProjectCode(code)
                .withProjectName(params.projectName())
                .withDescription(params.description())
                .withClientId(params.clientId())
                .withOriginOpportunityId(o.getId())
                .withType(params.type())
                .withPriority(params.priority())
                .withProjectManagerId(params.projectManagerId())
                .withAssignedSalesmanId(params.assignedSalesmanId())
                .withPlannedStartDate(params.plannedStartDate())
                .withPlannedEndDate(params.plannedEndDate())
                .withContractedValue(params.contractedValue())
                .withEstimatedCost(params.estimatedCost())
                .register());

    if (o.getStatus() == OpportunityStatus.LOST || o.getStatus() == OpportunityStatus.ABANDONED) {
      throw new IllegalStateException("Cannot win opportunity in state " + o.getStatus());
    }
    o.setStatus(OpportunityStatus.WON);
    o.setProbabilityPercent(100);
    o.setActualCloseDate(LocalDate.now(ClientTimeZones.current()));
    o.setConvertedProjectId(savedProject.getId());
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info(
        "opportunity win complete opportunityId={} projectId={} projectCode={}",
        saved.getId(),
        savedProject.getId(),
        code);
    return saved;
  }

  @Override
  public Opportunity lose(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Loss reason is required");
    }
    log.info(
        "opportunity lose start opportunityId={} reasonLen={}",
        id,
        reason.length());

    Opportunity o = getById(id);
    if (o.getStatus() == OpportunityStatus.WON) {
      throw new IllegalStateException("Cannot lose an opportunity already won");
    }
    o.setStatus(OpportunityStatus.LOST);
    o.setLostReason(reason);
    o.setProbabilityPercent(0);
    o.setActualCloseDate(LocalDate.now(ClientTimeZones.current()));
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity lose complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity abandon(Long id) {
    log.info("opportunity abandon start opportunityId={}", id);

    Opportunity o = getById(id);
    o.setStatus(OpportunityStatus.ABANDONED);
    o.setProbabilityPercent(0);
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity abandon complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  public Opportunity reopen(Long id) {
    log.info("opportunity reopen start opportunityId={}", id);

    Opportunity o = getById(id);
    if (o.getStatus() != OpportunityStatus.LOST && o.getStatus() != OpportunityStatus.ABANDONED) {
      throw new IllegalStateException("Only LOST or ABANDONED opportunities can be reopened");
    }
    o.setStatus(OpportunityStatus.DISCOVERY);
    o.setLostReason(null);
    o.setActualCloseDate(null);
    o.touch();

    Opportunity saved = opportunityRepository.save(o);
    log.info("opportunity reopen complete opportunityId={}", saved.getId());
    return saved;
  }
}
