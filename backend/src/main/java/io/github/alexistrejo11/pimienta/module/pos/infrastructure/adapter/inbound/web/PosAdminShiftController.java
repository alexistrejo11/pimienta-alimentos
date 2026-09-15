package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosShiftListFilter;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminShifts;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosShiftDetailResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosShiftListItemResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosShiftReconciliationResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/pos/admin/shifts")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminShifts
public class PosAdminShiftController {

  private final PosShiftAdminUseCases useCases;
  private final HeadquarterAccessService access;

  public PosAdminShiftController(PosShiftAdminUseCases useCases, HeadquarterAccessService access) {
    this.useCases = useCases;
    this.access = access;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public PagedResponse<PosShiftListItemResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam(required = false) Long headquarterId,
      @Parameter(
              description =
                  "Range start (inclusive). Filters closedAt when status=CLOSED or omitted with dates; filters openedAt when status=OPEN.")
          @RequestParam(required = false)
          Instant from,
      @Parameter(description = "Range end (exclusive). Same semantics as from.")
          @RequestParam(required = false)
          Instant to,
      @Parameter(description = "OPEN or CLOSED") @RequestParam(required = false) String status,
      @RequestParam(required = false) Long cashierOperatorId,
      @ModelAttribute PageableRequest pageable) {
    var filter = new PosShiftListFilter(from, to, status, cashierOperatorId);
    return PagedResponse.map(
        useCases.list(access.enforceHeadquarterScope(principal, headquarterId), filter, pageable.toPageable()),
        PosShiftListItemResponse::from);
  }

  @GetMapping("/{shiftId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public PosShiftDetailResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable UUID shiftId,
      @RequestParam long headquarterId) {
    long hq = access.enforceHeadquarterFilter(principal, headquarterId);
    return PosShiftDetailResponse.from(useCases.get(shiftId, hq));
  }

  @GetMapping("/{shiftId}/reconciliation")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  public PosShiftReconciliationResponse reconciliation(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable UUID shiftId,
      @RequestParam long headquarterId) {
    long hq = access.enforceHeadquarterFilter(principal, headquarterId);
    return PosShiftReconciliationResponse.from(useCases.getReconciliation(shiftId, hq));
  }
}
