package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.PosSaleCategoryManagementUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosSaleCategory;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSaleCategoryRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSaleCategoryResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/headquarters/{id}/pos-categories")
@RateLimit(profile = RateLimitProfile.STANDARD)
public class PosSaleCategoryController {
  private final PosSaleCategoryManagementUseCases useCases;
  private final HeadquarterAccessService access;
  public PosSaleCategoryController(PosSaleCategoryManagementUseCases useCases, HeadquarterAccessService access) { this.useCases = useCases; this.access = access; }
  @GetMapping public List<PosSaleCategoryResponse> list(@AuthenticationPrincipal JwtAuthenticationContext p, @PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeInactive) { access.requireHeadquarterAccess(p, id); return useCases.list(id, includeInactive).stream().map(PosSaleCategoryController::response).toList(); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS) public PosSaleCategoryResponse create(@AuthenticationPrincipal JwtAuthenticationContext p, @PathVariable Long id, @Valid @RequestBody PosSaleCategoryRequest r) { access.requireHeadquarterAccess(p, id); return response(useCases.create(id, r.name(), r.displayOrder() == null ? 0 : r.displayOrder())); }
  @PutMapping("/{categoryId}") @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS) public PosSaleCategoryResponse update(@AuthenticationPrincipal JwtAuthenticationContext p, @PathVariable Long id, @PathVariable Long categoryId, @Valid @RequestBody PosSaleCategoryRequest r) { access.requireHeadquarterAccess(p, id); var c = useCases.rename(id, categoryId, r.name()); if (r.displayOrder() != null) c = useCases.reorder(id, categoryId, r.displayOrder()); return response(c); }
  @DeleteMapping("/{categoryId}") @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS) public void archive(@AuthenticationPrincipal JwtAuthenticationContext p, @PathVariable Long id, @PathVariable Long categoryId) { access.requireHeadquarterAccess(p, id); useCases.archive(id, categoryId); }
  private static PosSaleCategoryResponse response(PosSaleCategory c) { return new PosSaleCategoryResponse(c.getId(), c.getHeadquarterId(), c.getName(), c.getDisplayOrder(), c.isActive()); }
}
