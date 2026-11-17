package io.github.alexistrejo11.pimienta.module.opportunity.core.application;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.CreateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpdateRequirementCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityRequirement;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityRuleViolationException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.RequirementNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityRequirementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRequirementRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityRequirementUseCasesImpl implements OpportunityRequirementUseCases {

  private static final Logger log =
      LoggerFactory.getLogger(OpportunityRequirementUseCasesImpl.class);

  private static final Set<OpportunityStatus> TERMINAL =
      EnumSet.of(
          OpportunityStatus.WON,
          OpportunityStatus.LOST,
          OpportunityStatus.ABANDONED,
          OpportunityStatus.CANCELLED);

  private final OpportunityRequirementRepository requirementRepository;
  private final OpportunityRepository opportunityRepository;

  public OpportunityRequirementUseCasesImpl(
      OpportunityRequirementRepository requirementRepository,
      OpportunityRepository opportunityRepository) {
    this.requirementRepository = requirementRepository;
    this.opportunityRepository = opportunityRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OpportunityRequirement> listByOpportunity(long opportunityId) {
    requireOpportunity(opportunityId);
    return requirementRepository.findByOpportunityId(opportunityId);
  }

  @Override
  @Transactional(readOnly = true)
  public OpportunityRequirement getById(long opportunityId, long requirementId) {
    requireOpportunity(opportunityId);
    return requireOwned(opportunityId, requirementId);
  }

  @Override
  @Transactional
  public OpportunityRequirement create(long opportunityId, CreateRequirementCommand command) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    int sortOrder =
        command.sortOrder() != null
            ? command.sortOrder()
            : requirementRepository.findByOpportunityId(opportunityId).size();
    RequirementCategory category =
        command.category() != null ? command.category() : RequirementCategory.OTHER;
    log.info("create requirement opportunityId={} title={}", opportunityId, command.title());
    return requirementRepository.save(
        OpportunityRequirement.builder()
            .withOpportunityId(opportunityId)
            .withTitle(command.title())
            .withCategory(category)
            .withMandatory(command.mandatory())
            .withDueOn(command.dueOn())
            .withNotes(command.notes())
            .withSortOrder(sortOrder)
            .register());
  }

  @Override
  @Transactional
  public OpportunityRequirement update(
      long opportunityId, long requirementId, UpdateRequirementCommand command) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    OpportunityRequirement existing = requireOwned(opportunityId, requirementId);
    RequirementCategory category =
        command.category() != null ? command.category() : RequirementCategory.OTHER;
    int sortOrder = command.sortOrder() != null ? command.sortOrder() : existing.getSortOrder();
    existing.applyDetails(
        command.title(),
        category,
        command.mandatory(),
        command.dueOn(),
        command.notes(),
        sortOrder);
    return requirementRepository.save(existing);
  }

  @Override
  @Transactional
  public OpportunityRequirement changeStatus(
      long opportunityId, long requirementId, RequirementStatus status) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    if (status == null || status == RequirementStatus.UNDEFINED) {
      throw new OpportunityRuleViolationException(
          "A valid requirement status is required.",
          Map.of("requirementId", requirementId),
          "invalid requirement status");
    }
    OpportunityRequirement existing = requireOwned(opportunityId, requirementId);
    existing.changeStatus(status);
    return requirementRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(long opportunityId, long requirementId) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    OpportunityRequirement existing = requireOwned(opportunityId, requirementId);
    existing.delete();
    requirementRepository.save(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public long countBlockingMandatory(long opportunityId) {
    return requirementRepository.countMandatoryNotSatisfied(opportunityId);
  }

  private Opportunity requireOpportunity(long opportunityId) {
    return opportunityRepository
        .findById(opportunityId)
        .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
  }

  private OpportunityRequirement requireOwned(long opportunityId, long requirementId) {
    OpportunityRequirement requirement =
        requirementRepository
            .findById(requirementId)
            .orElseThrow(() -> new RequirementNotFoundException(requirementId));
    if (!requirement.getOpportunityId().equals(opportunityId)) {
      throw new RequirementNotFoundException(requirementId);
    }
    return requirement;
  }

  private void ensureMutable(Opportunity opportunity) {
    if (TERMINAL.contains(opportunity.getStatus())
        || opportunity.getStatus() == OpportunityStatus.SUBMITTED) {
      throw new OpportunityRuleViolationException(
          "Requirements cannot be changed after the opportunity is submitted or closed.",
          Map.of("opportunityId", opportunity.getId(), "status", opportunity.getStatus().name()),
          "requirement mutation blocked status=" + opportunity.getStatus());
    }
  }
}
