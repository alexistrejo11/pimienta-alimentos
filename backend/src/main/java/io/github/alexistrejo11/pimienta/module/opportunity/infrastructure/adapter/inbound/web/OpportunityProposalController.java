package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityProposalUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityProposals;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocProposalCreate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocProposalDelete;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocProposalList;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocProposalUpdate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.ProposalResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpsertProposalRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper.ProposalWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/opportunities/{opportunityId}/proposals")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOpportunityProposals
public class OpportunityProposalController {

  private final OpportunityProposalUseCases proposalUseCases;

  public OpportunityProposalController(OpportunityProposalUseCases proposalUseCases) {
    this.proposalUseCases = proposalUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocProposalList
  public List<ProposalResponse> list(@PathVariable long opportunityId) {
    return proposalUseCases.listByOpportunity(opportunityId).stream()
        .map(ProposalWebMapper::toResponse)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProposalCreate
  public ProposalResponse create(
      @PathVariable long opportunityId, @Valid @RequestBody UpsertProposalRequest request) {
    return ProposalWebMapper.toResponse(
        proposalUseCases.create(opportunityId, ProposalWebMapper.toCommand(request)));
  }

  @PutMapping("/{proposalId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProposalUpdate
  public ProposalResponse update(
      @PathVariable long opportunityId,
      @PathVariable long proposalId,
      @Valid @RequestBody UpsertProposalRequest request) {
    return ProposalWebMapper.toResponse(
        proposalUseCases.update(opportunityId, proposalId, ProposalWebMapper.toCommand(request)));
  }

  @DeleteMapping("/{proposalId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocProposalDelete
  public void delete(@PathVariable long opportunityId, @PathVariable long proposalId) {
    proposalUseCases.delete(opportunityId, proposalId);
  }
}
