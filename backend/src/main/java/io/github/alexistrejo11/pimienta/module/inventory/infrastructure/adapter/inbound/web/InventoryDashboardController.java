package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.InventoryDashboard;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryDashboard;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocInventoryDashboardGet;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryDashboardResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/inventory/dashboard")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocInventoryDashboard
public class InventoryDashboardController {

  private final InventoryManagementUseCases inventoryManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public InventoryDashboardController(
      InventoryManagementUseCases inventoryManagementUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.inventoryManagementUseCases = inventoryManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocInventoryDashboardGet
  public InventoryDashboardResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam(required = false) Long headquarterId) {
    var scope = headquarterAccessService.enforceHeadquarterScope(principal, headquarterId);
    InventoryDashboard dashboard =
        scope != null && scope.isEmpty()
            ? InventoryDashboard.empty()
            : inventoryManagementUseCases.dashboard(scope);
    return new InventoryDashboardResponse(
        dashboard.skuCount(),
        dashboard.lowStockCount(),
        dashboard.outOfStockCount(),
        dashboard.openCountSessionCount(),
        dashboard.totalAvailableQuantity(),
        dashboard.totalStockValue());
  }
}
