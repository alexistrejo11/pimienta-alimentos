package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OpportunityRequirementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOpportunityRequirements;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocRequirementCreate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocRequirementDelete;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocRequirementList;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocRequirementStatus;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocRequirementUpdate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.CreateRequirementRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.RequirementResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpdateRequirementRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpdateRequirementStatusRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper.RequirementWebMapper;
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
@RequestMapping(BASE + "/opportunities/{opportunityId}/requirements")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOpportunityRequirements
public class OpportunityRequirementController {

  private final OpportunityRequirementUseCases requirementUseCases;

  public OpportunityRequirementController(OpportunityRequirementUseCases requirementUseCases) {
    this.requirementUseCases = requirementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocRequirementList
  public List<RequirementResponse> list(@PathVariable long opportunityId) {
    return requirementUseCases.listByOpportunity(opportunityId).stream()
        .map(RequirementWebMapper::toResponse)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocRequirementCreate
  public RequirementResponse create(
      @PathVariable long opportunityId, @Valid @RequestBody CreateRequirementRequest request) {
    return RequirementWebMapper.toResponse(
        requirementUseCases.create(opportunityId, RequirementWebMapper.toCreateCommand(request)));
  }

  @PutMapping("/{requirementId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocRequirementUpdate
  public RequirementResponse update(
      @PathVariable long opportunityId,
      @PathVariable long requirementId,
      @Valid @RequestBody UpdateRequirementRequest request) {
    return RequirementWebMapper.toResponse(
        requirementUseCases.update(
            opportunityId, requirementId, RequirementWebMapper.toUpdateCommand(request)));
  }

  @PutMapping("/{requirementId}/status")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocRequirementStatus
  public RequirementResponse changeStatus(
      @PathVariable long opportunityId,
      @PathVariable long requirementId,
      @Valid @RequestBody UpdateRequirementStatusRequest request) {
    return RequirementWebMapper.toResponse(
        requirementUseCases.changeStatus(opportunityId, requirementId, request.status()));
  }

  @DeleteMapping("/{requirementId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocRequirementDelete
  public void delete(@PathVariable long opportunityId, @PathVariable long requirementId) {
    requirementUseCases.delete(opportunityId, requirementId);
  }
}
