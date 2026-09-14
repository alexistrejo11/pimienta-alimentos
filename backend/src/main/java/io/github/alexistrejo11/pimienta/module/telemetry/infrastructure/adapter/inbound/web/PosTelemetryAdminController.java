package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.doc.DocPosTelemetryAdmin;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.doc.DocPosTelemetryEndpoint;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryActivityResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryDashboardResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryDeviceResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/pos/admin/telemetry")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosTelemetryAdmin
public class PosTelemetryAdminController {
  private final TelemetryObservabilityService service;
  private final HeadquarterAccessService access;

  public PosTelemetryAdminController(TelemetryObservabilityService service, HeadquarterAccessService access) {
    this.service = service;
    this.access = access;
  }

  @GetMapping("/dashboard")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosTelemetryEndpoint
  public PosTelemetryDashboardResponse dashboard(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam(required = false) Long headquarterId) {
    Long hq = access.enforceHeadquarterFilter(principal, headquarterId);
    return service.dashboard(hq);
  }

  @GetMapping("/devices/{deviceId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosTelemetryEndpoint
  public PosTelemetryDeviceResponse device(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable UUID deviceId, @ModelAttribute PageableRequest pageable) {
    var response = service.device(deviceId, pageable.toPageable());
    access.requireHeadquarterAccess(principal, response.headquarterId());
    return response;
  }

  @GetMapping("/activity")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosTelemetryEndpoint
  public PosTelemetryActivityResponse activity(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam(required = false) Long headquarterId,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "50") int limit) {
    Long hq = access.enforceHeadquarterFilter(principal, headquarterId);
    return service.activity(hq, cursor, limit);
  }
}
