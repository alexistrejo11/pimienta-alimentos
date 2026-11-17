package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityManagementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunities;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityAbandon;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityCancel;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityCreate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityDelete;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityGet;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityList;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityMarkLost;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityMarkSubmitted;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityMarkWon;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityStartPreparing;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunitySubmitForReview;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityUpdate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.CreateOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OpportunityResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OpportunitySearchRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpdateOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper.OpportunityWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/opportunities")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOpportunities
public class OpportunityController {

  private final OpportunityManagementUseCases opportunityUseCases;

  public OpportunityController(OpportunityManagementUseCases opportunityUseCases) {
    this.opportunityUseCases = opportunityUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityList
  public PagedResponse<OpportunityResponse> list(@ModelAttribute OpportunitySearchRequest filter) {
    OpportunitySearchRequest query = filter != null ? filter : new OpportunitySearchRequest();
    return PagedResponse.map(
        opportunityUseCases.search(
            OpportunityWebMapper.toCriteria(query),
            PageRequest.of(query.getPage(), query.getSize())),
        OpportunityWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityGet
  public OpportunityResponse get(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.getById(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityCreate
  public OpportunityResponse create(@Valid @RequestBody CreateOpportunityRequest request) {
    return OpportunityWebMapper.toResponse(
        opportunityUseCases.create(OpportunityWebMapper.toCreateCommand(request)));
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityUpdate
  public OpportunityResponse update(
      @PathVariable long id, @Valid @RequestBody UpdateOpportunityRequest request) {
    return OpportunityWebMapper.toResponse(
        opportunityUseCases.update(id, OpportunityWebMapper.toUpdateCommand(request)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityDelete
  public void delete(@PathVariable long id) {
    opportunityUseCases.delete(id);
  }

  @PostMapping("/{id}/submit-for-review")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunitySubmitForReview
  public OpportunityResponse submitForReview(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.submitForReview(id));
  }

  @PostMapping("/{id}/start-preparing")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityStartPreparing
  public OpportunityResponse startPreparing(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.startPreparing(id));
  }

  @PostMapping("/{id}/mark-submitted")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityMarkSubmitted
  public OpportunityResponse markSubmitted(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.markSubmitted(id));
  }

  @PostMapping("/{id}/mark-won")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityMarkWon
  public OpportunityResponse markWon(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.markWon(id));
  }

  @PostMapping("/{id}/mark-lost")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityMarkLost
  public OpportunityResponse markLost(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.markLost(id));
  }

  @PostMapping("/{id}/abandon")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityAbandon
  public OpportunityResponse abandon(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.abandon(id));
  }

  @PostMapping("/{id}/cancel")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityCancel
  public OpportunityResponse cancel(@PathVariable long id) {
    return OpportunityWebMapper.toResponse(opportunityUseCases.cancel(id));
  }
}
