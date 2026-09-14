package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;
import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosShiftAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosShiftDetailResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosShiftResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit; import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile; import io.github.alexistrejo11.pimienta.shared.web.PageableRequest; import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import java.util.UUID; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.GetMapping; import org.springframework.web.bind.annotation.ModelAttribute; import org.springframework.web.bind.annotation.PathVariable; import org.springframework.web.bind.annotation.RequestMapping; import org.springframework.web.bind.annotation.RequestParam; import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping(BASE+"/pos/admin/shifts") @RateLimit(profile=RateLimitProfile.STANDARD)
public class PosAdminShiftController {
  private final PosShiftAdminUseCases useCases; private final HeadquarterAccessService access;
  public PosAdminShiftController(PosShiftAdminUseCases useCases,HeadquarterAccessService access){this.useCases=useCases;this.access=access;}
  @GetMapping @RateLimit(profile=RateLimitProfile.READ_HEAVY)
  public PagedResponse<PosShiftResponse> list(@AuthenticationPrincipal JwtAuthenticationContext principal,@RequestParam(required=false) Long headquarterId,@ModelAttribute PageableRequest pageable){return PagedResponse.map(useCases.list(access.enforceHeadquarterScope(principal,headquarterId),pageable.toPageable()),PosShiftResponse::from);}
  @GetMapping("/{shiftId}") @RateLimit(profile=RateLimitProfile.READ_HEAVY)
  public PosShiftDetailResponse get(@AuthenticationPrincipal JwtAuthenticationContext principal,@PathVariable UUID shiftId,@RequestParam long headquarterId){long hq=access.enforceHeadquarterFilter(principal,headquarterId);return PosShiftDetailResponse.from(useCases.get(shiftId,hq));}
  @GetMapping("/{shiftId}/reconciliation") @RateLimit(profile=RateLimitProfile.READ_HEAVY)
  public PosShiftDetailResponse reconciliation(@AuthenticationPrincipal JwtAuthenticationContext principal,@PathVariable UUID shiftId,@RequestParam long headquarterId){return get(principal,shiftId,headquarterId);}
}
