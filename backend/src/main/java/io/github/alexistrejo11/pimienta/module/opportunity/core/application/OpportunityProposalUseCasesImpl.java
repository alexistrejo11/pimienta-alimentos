package io.github.alexistrejo11.pimienta.module.opportunity.core.application;

import io.github.alexistrejo11.pimienta.module.opportunity.core.application.command.UpsertProposalCommand;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.OpportunityProposal;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.ProposalKind;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.OpportunityRuleViolationException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.exception.ProposalNotFoundException;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityProposalUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityProposalRepository;
import io.github.alexistrejo11.pimienta.module.opportunity.core.port.output.OpportunityRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityProposalUseCasesImpl implements OpportunityProposalUseCases {

  private static final Logger log = LoggerFactory.getLogger(OpportunityProposalUseCasesImpl.class);

  private static final Set<OpportunityStatus> TERMINAL =
      EnumSet.of(
          OpportunityStatus.WON,
          OpportunityStatus.LOST,
          OpportunityStatus.ABANDONED,
          OpportunityStatus.CANCELLED);

  private final OpportunityProposalRepository proposalRepository;
  private final OpportunityRepository opportunityRepository;

  public OpportunityProposalUseCasesImpl(
      OpportunityProposalRepository proposalRepository,
      OpportunityRepository opportunityRepository) {
    this.proposalRepository = proposalRepository;
    this.opportunityRepository = opportunityRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<OpportunityProposal> listByOpportunity(long opportunityId) {
    requireOpportunity(opportunityId);
    return proposalRepository.findByOpportunityId(opportunityId);
  }

  @Override
  @Transactional(readOnly = true)
  public OpportunityProposal getById(long opportunityId, long proposalId) {
    requireOpportunity(opportunityId);
    return requireOwned(opportunityId, proposalId);
  }

  @Override
  @Transactional
  public OpportunityProposal create(long opportunityId, UpsertProposalCommand command) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    ProposalKind kind = command.kind() != null ? command.kind() : ProposalKind.COMBINED;
    log.info("create proposal opportunityId={} kind={}", opportunityId, kind);
    return proposalRepository.save(
        OpportunityProposal.builder()
            .withOpportunityId(opportunityId)
            .withKind(kind)
            .withNotes(command.notes())
            .register());
  }

  @Override
  @Transactional
  public OpportunityProposal update(
      long opportunityId, long proposalId, UpsertProposalCommand command) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    OpportunityProposal existing = requireOwned(opportunityId, proposalId);
    ProposalKind kind = command.kind() != null ? command.kind() : ProposalKind.COMBINED;
    existing.applyDetails(kind, command.notes());
    return proposalRepository.save(existing);
  }

  @Override
  @Transactional
  public void delete(long opportunityId, long proposalId) {
    Opportunity opportunity = requireOpportunity(opportunityId);
    ensureMutable(opportunity);
    OpportunityProposal existing = requireOwned(opportunityId, proposalId);
    existing.delete();
    proposalRepository.save(existing);
  }

  private Opportunity requireOpportunity(long opportunityId) {
    return opportunityRepository
        .findById(opportunityId)
        .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
  }

  private OpportunityProposal requireOwned(long opportunityId, long proposalId) {
    OpportunityProposal proposal =
        proposalRepository
            .findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));
    if (!proposal.getOpportunityId().equals(opportunityId)) {
      throw new ProposalNotFoundException(proposalId);
    }
    return proposal;
  }

  private void ensureMutable(Opportunity opportunity) {
    if (TERMINAL.contains(opportunity.getStatus())
        || opportunity.getStatus() == OpportunityStatus.SUBMITTED) {
      throw new OpportunityRuleViolationException(
          "Proposals cannot be changed after the opportunity is submitted or closed.",
          Map.of("opportunityId", opportunity.getId(), "status", opportunity.getStatus().name()),
          "proposal mutation blocked status=" + opportunity.getStatus());
    }
  }
}
