package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryCountCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryCountUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.StorageLocationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryCountSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryCountSessions;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryCountResponseRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InventoryCountSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.OpenInventoryCountRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryCountResponseDto;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryCountSessionResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryCountSessionSummaryResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping(BASE + "/inventory/count-sessions")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryCountSessions
public class InventoryCountController {

  private final InventoryCountUseCases useCases;
  private final StorageLocationManagementUseCases locations;
  private final HeadquarterAccessService access;

  public InventoryCountController(
      InventoryCountUseCases useCases,
      StorageLocationManagementUseCases locations,
      HeadquarterAccessService access) {
    this.useCases = useCases;
    this.locations = locations;
    this.access = access;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryCountSearch
  public PagedResponse<InventoryCountSessionSummaryResponse> search(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute InventoryCountSearchRequest filter) {
    var hqScope = access.enforceHeadquarterScope(principal, filter.getHeadquarterId());
    Page<InventoryCountSession> page =
        useCases.search(filter.toCriteria(hqScope), filter.toPageable());
    return PagedResponse.map(page, InventoryCountController::toSummary);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public InventoryCountSessionResponse open(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody OpenInventoryCountRequest request) {
    requireLocationAccess(principal, request.locationId());
    InventoryCountSession session =
        useCases.open(
            new InventoryCountCommands.Open(
                request.locationId(), request.type(), request.itemIds(), principal.userId()));
    return toResponse(session, principal, false);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public InventoryCountSessionResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    InventoryCountSession session = useCases.get(id);
    requireLocationAccess(principal, session.getLocationId());
    return toResponse(session, principal, shouldReveal(session, principal));
  }

  @PostMapping("/{id}/responses")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public InventoryCountSessionResponse respond(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable long id,
      @Valid @RequestBody InventoryCountResponseRequest request) {
    InventoryCountSession session = useCases.get(id);
    requireLocationAccess(principal, session.getLocationId());
    session =
        useCases.respond(id, new InventoryCountCommands.Response(request.itemId(), request.countedQuantity()));
    return toResponse(session, principal, false);
  }

  @PostMapping("/{id}/submit")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public InventoryCountSessionResponse submit(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    InventoryCountSession session = useCases.get(id);
    requireLocationAccess(principal, session.getLocationId());
    session = useCases.submit(id, principal.userId());
    return toResponse(session, principal, shouldReveal(session, principal));
  }

  @PostMapping("/{id}/approve")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public InventoryCountSessionResponse approve(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    InventoryCountSession session = useCases.get(id);
    requireLocationAccess(principal, session.getLocationId());
    session = useCases.approve(id, principal.userId());
    return toResponse(session, principal, true);
  }

  @PostMapping("/{id}/cancel")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  public void cancel(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    InventoryCountSession session = useCases.get(id);
    requireLocationAccess(principal, session.getLocationId());
    useCases.cancel(id);
  }

  private void requireLocationAccess(JwtAuthenticationContext principal, long locationId) {
    access.requireOwnedLocation(
        principal, locations.getById(locationId).getHeadquarterId());
  }

  private static boolean shouldReveal(
      InventoryCountSession session, JwtAuthenticationContext principal) {
    if (session.getStatus() == InventoryCountSession.Status.APPROVED) {
      return true;
    }
    if (session.getStatus() == InventoryCountSession.Status.SUBMITTED
        && accessIsAdmin(principal)) {
      return true;
    }
    return false;
  }

  private static boolean accessIsAdmin(JwtAuthenticationContext principal) {
    return principal.roles().contains("ADMIN") || principal.roles().contains("ROLE_ADMIN");
  }

  private InventoryCountSessionResponse toResponse(
      InventoryCountSession session, JwtAuthenticationContext principal, boolean reveal) {
    return new InventoryCountSessionResponse(
        session.getId(),
        session.getLocationId(),
        session.getCountType(),
        session.getStatus(),
        session.getCreatedById(),
        session.getSubmittedById(),
        session.getApprovedById(),
        session.getCreatedAt(),
        session.getSubmittedAt(),
        session.getApprovedAt(),
        session.getCancelledAt(),
        session.getResponses().stream()
            .map(
                row ->
                    new InventoryCountResponseDto(
                        row.getItemId(),
                        reveal ? row.getExpectedQuantity() : null,
                        row.getCountedQuantity(),
                        reveal ? row.getVariance() : null))
            .toList());
  }

  private static InventoryCountSessionSummaryResponse toSummary(InventoryCountSession session) {
    return new InventoryCountSessionSummaryResponse(
        session.getId(),
        session.getLocationId(),
        session.getCountType(),
        session.getStatus(),
        session.getCreatedById(),
        session.getSubmittedById(),
        session.getApprovedById(),
        session.getCreatedAt(),
        session.getSubmittedAt(),
        session.getApprovedAt(),
        session.getCancelledAt());
  }
}
