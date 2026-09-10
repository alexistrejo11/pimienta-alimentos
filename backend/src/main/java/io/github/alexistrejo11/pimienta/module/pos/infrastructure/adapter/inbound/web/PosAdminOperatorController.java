package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.OperatorManagementUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminOperators;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorAssignHq;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorCreate;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorGet;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorList;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorUnassignHq;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosOperatorUpdate;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.AssignOperatorHeadquarterRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.CreatePosOperatorRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosOperatorResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.UpdatePosOperatorRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/admin/operators")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminOperators
public class PosAdminOperatorController {

  private final OperatorManagementUseCases operatorManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public PosAdminOperatorController(
      OperatorManagementUseCases operatorManagementUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.operatorManagementUseCases = operatorManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosOperatorList
  public PagedResponse<PosOperatorResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ModelAttribute PageableRequest pageable) {
    if (headquarterAccessService.isManager(principal) && !headquarterAccessService.isAdmin(principal)) {
      headquarterAccessService.resolveManagerHeadquarter(principal);
    }
    return PagedResponse.map(
        operatorManagementUseCases.list(pageable.toPageable()), PosWebMapper::toOperatorResponse);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosOperatorGet
  public PosOperatorResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable("id") long id) {
    PosOperator operator = operatorManagementUseCases.get(id);
    requireOperatorAccess(principal, operator);
    return PosWebMapper.toOperatorResponse(operator);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosOperatorCreate
  public PosOperatorResponse create(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody CreatePosOperatorRequest request) {
    if (request.headquarterIds() != null) {
      for (Long hqId : request.headquarterIds()) {
        headquarterAccessService.requireHeadquarterAccess(principal, hqId);
      }
    }
    PosOperator created =
        operatorManagementUseCases.create(PosWebMapper.toCreateOperatorCommand(request));
    return PosWebMapper.toOperatorResponse(created);
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosOperatorUpdate
  public PosOperatorResponse update(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") long id,
      @Valid @RequestBody UpdatePosOperatorRequest request) {
    PosOperator existing = operatorManagementUseCases.get(id);
    requireOperatorAccess(principal, existing);
    return PosWebMapper.toOperatorResponse(
        operatorManagementUseCases.update(id, PosWebMapper.toUpdateOperatorCommand(request)));
  }

  @PostMapping("/{id}/headquarters")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosOperatorAssignHq
  public PosOperatorResponse assignHeadquarter(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") long id,
      @Valid @RequestBody AssignOperatorHeadquarterRequest request) {
    headquarterAccessService.requireHeadquarterAccess(principal, request.headquarterId());
    return PosWebMapper.toOperatorResponse(
        operatorManagementUseCases.assignHeadquarter(id, request.headquarterId()));
  }

  @DeleteMapping("/{id}/headquarters/{headquarterId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosOperatorUnassignHq
  public PosOperatorResponse unassignHeadquarter(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") long id,
      @PathVariable("headquarterId") long headquarterId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    return PosWebMapper.toOperatorResponse(
        operatorManagementUseCases.unassignHeadquarter(id, headquarterId));
  }

  private void requireOperatorAccess(JwtAuthenticationContext principal, PosOperator operator) {
    if (headquarterAccessService.isAdmin(principal)) {
      return;
    }
    Long managerHq = headquarterAccessService.resolveManagerHeadquarter(principal);
    if (!operator.getHeadquarterIds().contains(managerHq)) {
      headquarterAccessService.requireHeadquarterAccess(principal, managerHq);
    }
  }
}
