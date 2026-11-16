package io.github.alexistrejo11.pimienta.module.opportunity.core.application;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateOpportunityCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityRuleViolationException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OrganizationNotFoundException;
import io.github.alexistrejo11.pimienta.module.contract.core.application.command.CreateContractCommand;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.Contract;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractCategory;
import io.github.alexistrejo11.pimienta.module.contract.core.domain.enums.ContractTermKind;
import io.github.alexistrejo11.pimienta.module.contract.core.port.ContractManagementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityManagementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRequirementRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OrganizationRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityManagementUseCasesImpl implements OpportunityManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(OpportunityManagementUseCasesImpl.class);

  private static final Set<OpportunityStatus> TERMINAL =
      EnumSet.of(
          OpportunityStatus.WON,
          OpportunityStatus.LOST,
          OpportunityStatus.ABANDONED,
          OpportunityStatus.CANCELLED);

  private final OpportunityRepository opportunityRepository;
  private final OrganizationRepository organizationRepository;
  private final OpportunityRequirementRepository requirementRepository;
  private final ContractManagementUseCases contractManagementUseCases;

  public OpportunityManagementUseCasesImpl(
      OpportunityRepository opportunityRepository,
      OrganizationRepository organizationRepository,
      OpportunityRequirementRepository requirementRepository,
      ContractManagementUseCases contractManagementUseCases) {
    this.opportunityRepository = opportunityRepository;
    this.organizationRepository = organizationRepository;
    this.requirementRepository = requirementRepository;
    this.contractManagementUseCases = contractManagementUseCases;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Opportunity> search(OpportunitySearchCriteria criteria, Pageable pageable) {
    return opportunityRepository.search(
        criteria != null ? criteria : OpportunitySearchCriteria.empty(), pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Opportunity getById(long id) {
    return opportunityRepository.findById(id).orElseThrow(() -> new OpportunityNotFoundException(id));
  }

  @Override
  @Transactional
  public Opportunity create(CreateOpportunityCommand command) {
    requireOrganization(command.organizationId());
    log.info(
        "create opportunity start organizationId={} title={}",
        command.organizationId(),
        command.title());
    Opportunity saved =
        opportunityRepository.save(
            Opportunity.builder()
                .withOrganizationId(command.organizationId())
                .withTitle(command.title())
                .withSourceUrl(command.sourceUrl())
                .withExternalReference(command.externalReference())
                .withPublishedOn(command.publishedOn())
                .withSubmissionDeadline(command.submissionDeadline())
                .withDecisionOn(command.decisionOn())
                .withNotes(command.notes())
                .register());
    log.info("create opportunity complete opportunityId={}", saved.getId());
    return saved;
  }

  @Override
  @Transactional
  public Opportunity update(long id, UpdateOpportunityCommand command) {
    Opportunity existing = getById(id);
    ensureEditable(existing);
    requireOrganization(command.organizationId());
    existing.applyDetails(
        command.organizationId(),
        command.title(),
        command.sourceUrl(),
        command.externalReference(),
        command.publishedOn(),
        command.submissionDeadline(),
        command.decisionOn(),
        command.notes());
    return opportunityRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(long id) {
    Opportunity existing = getById(id);
    if (existing.getStatus() != OpportunityStatus.DRAFT) {
      throw new OpportunityRuleViolationException(
          "Only draft opportunities can be deleted.",
          Map.of("opportunityId", id, "status", existing.getStatus().name()),
          "delete opportunity blocked status=" + existing.getStatus());
    }
    existing.delete();
    opportunityRepository.save(existing);
  }

  @Override
  @Transactional
  public Opportunity submitForReview(long id) {
    return transition(id, OpportunityStatus.DRAFT, OpportunityStatus.UNDER_REVIEW, false, false);
  }

  @Override
  @Transactional
  public Opportunity startPreparing(long id) {
    return transition(
        id, OpportunityStatus.UNDER_REVIEW, OpportunityStatus.PREPARING, false, false);
  }

  @Override
  @Transactional
  public Opportunity markSubmitted(long id) {
    Opportunity existing = getById(id);
    if (existing.getStatus() != OpportunityStatus.PREPARING) {
      throw invalidTransition(existing, OpportunityStatus.SUBMITTED);
    }
    long blocking = requirementRepository.countMandatoryNotSatisfied(id);
    if (blocking > 0) {
      throw new OpportunityRuleViolationException(
          "No se puede marcar como enviada: hay requisitos obligatorios pendientes.",
          Map.of("opportunityId", id, "pendingMandatory", blocking),
          "markSubmitted blocked pendingMandatory=" + blocking);
    }
    existing.transitionTo(OpportunityStatus.SUBMITTED);
    existing.markSubmittedNow();
    return opportunityRepository.save(existing);
  }

  @Override
  @Transactional
  public Opportunity markWon(long id) {
    Opportunity existing = getById(id);
    if (existing.getStatus() != OpportunityStatus.SUBMITTED) {
      throw invalidTransition(existing, OpportunityStatus.WON);
    }
    if (existing.getContractId() == null) {
      Contract created = contractManagementUseCases.create(buildAwardContractCommand(existing));
      existing.linkContract(created.getId());
      log.info(
          "markWon linked contract opportunityId={} contractId={}", id, created.getId());
    }
    existing.transitionTo(OpportunityStatus.WON);
    existing.markClosedNow();
    return opportunityRepository.save(existing);
  }

  private CreateContractCommand buildAwardContractCommand(Opportunity opportunity) {
    String orgName =
        organizationRepository
            .findById(opportunity.getOrganizationId())
            .map(org -> org.getName())
            .orElse("Organización #" + opportunity.getOrganizationId());
    LocalDate start =
        opportunity.getDecisionOn() != null
            ? opportunity.getDecisionOn()
            : LocalDate.now();
    String documentUrl =
        opportunity.getSourceUrl() != null && !opportunity.getSourceUrl().isBlank()
            ? opportunity.getSourceUrl()
            : "pending://opportunity/" + opportunity.getId();
    String description =
        "Contrato institucional generado al adjudicar la convocatoria #"
            + opportunity.getId()
            + " ("
            + orgName
            + ").";
    return new CreateContractCommand(
        opportunity.getTitle(),
        description,
        ContractCategory.INSTITUTIONAL,
        null,
        ContractTermKind.INDEFINITE,
        start,
        null,
        documentUrl,
        opportunity.getNotes(),
        opportunity.getExternalReference(),
        null,
        null,
        null);
  }

  @Override
  @Transactional
  public Opportunity markLost(long id) {
    return transition(id, OpportunityStatus.SUBMITTED, OpportunityStatus.LOST, false, true);
  }

  @Override
  @Transactional
  public Opportunity abandon(long id) {
    Opportunity existing = getById(id);
    if (TERMINAL.contains(existing.getStatus())) {
      throw invalidTransition(existing, OpportunityStatus.ABANDONED);
    }
    existing.transitionTo(OpportunityStatus.ABANDONED);
    existing.markClosedNow();
    return opportunityRepository.save(existing);
  }

  @Override
  @Transactional
  public Opportunity cancel(long id) {
    Opportunity existing = getById(id);
    if (existing.getStatus() == OpportunityStatus.SUBMITTED
        || TERMINAL.contains(existing.getStatus())) {
      throw invalidTransition(existing, OpportunityStatus.CANCELLED);
    }
    existing.transitionTo(OpportunityStatus.CANCELLED);
    existing.markClosedNow();
    return opportunityRepository.save(existing);
  }

  private Opportunity transition(
      long id,
      OpportunityStatus expected,
      OpportunityStatus next,
      boolean markSubmitted,
      boolean markClosed) {
    Opportunity existing = getById(id);
    if (existing.getStatus() != expected) {
      throw invalidTransition(existing, next);
    }
    existing.transitionTo(next);
    if (markSubmitted) {
      existing.markSubmittedNow();
    }
    if (markClosed) {
      existing.markClosedNow();
    }
    return opportunityRepository.save(existing);
  }

  private void requireOrganization(long organizationId) {
    organizationRepository
        .findById(organizationId)
        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
  }

  private void ensureEditable(Opportunity opportunity) {
    if (TERMINAL.contains(opportunity.getStatus())) {
      throw new OpportunityRuleViolationException(
          "Terminal opportunities cannot be edited.",
          Map.of("opportunityId", opportunity.getId(), "status", opportunity.getStatus().name()),
          "edit blocked terminal status=" + opportunity.getStatus());
    }
  }

  private static OpportunityRuleViolationException invalidTransition(
      Opportunity opportunity, OpportunityStatus next) {
    return new OpportunityRuleViolationException(
        "Invalid opportunity status transition.",
        Map.of(
            "opportunityId",
            opportunity.getId(),
            "from",
            opportunity.getStatus().name(),
            "to",
            next.name()),
        "invalid transition from=" + opportunity.getStatus() + " to=" + next);
  }
}
