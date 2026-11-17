package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.opportunity.core.port.input.OrganizationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizationCreate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizationDelete;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizationGet;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizationList;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizationUpdate;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc.DocOrganizations;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OrganizationResponse;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OrganizationSearchRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.UpsertOrganizationRequest;
import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.mapper.OrganizationWebMapper;
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
@RequestMapping(BASE + "/organizations")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOrganizations
public class OrganizationController {

  private final OrganizationManagementUseCases organizationUseCases;

  public OrganizationController(OrganizationManagementUseCases organizationUseCases) {
    this.organizationUseCases = organizationUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOrganizationList
  public PagedResponse<OrganizationResponse> list(@ModelAttribute OrganizationSearchRequest filter) {
    OrganizationSearchRequest query = filter != null ? filter : new OrganizationSearchRequest();
    return PagedResponse.map(
        organizationUseCases.search(
            OrganizationWebMapper.toCriteria(query),
            PageRequest.of(query.getPage(), query.getSize())),
        OrganizationWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOrganizationGet
  public OrganizationResponse get(@PathVariable long id) {
    return OrganizationWebMapper.toResponse(organizationUseCases.getById(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOrganizationCreate
  public OrganizationResponse create(@Valid @RequestBody UpsertOrganizationRequest request) {
    return OrganizationWebMapper.toResponse(
        organizationUseCases.create(OrganizationWebMapper.toCommand(request)));
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOrganizationUpdate
  public OrganizationResponse update(
      @PathVariable long id, @Valid @RequestBody UpsertOrganizationRequest request) {
    return OrganizationWebMapper.toResponse(
        organizationUseCases.update(id, OrganizationWebMapper.toCommand(request)));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOrganizationDelete
  public void delete(@PathVariable long id) {
    organizationUseCases.delete(id);
  }
}
