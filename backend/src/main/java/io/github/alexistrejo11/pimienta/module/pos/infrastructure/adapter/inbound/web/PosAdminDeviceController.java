package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminDevices;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceList;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceRevoke;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosDeviceAdminResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/pos/admin/devices")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminDevices
public class PosAdminDeviceController {

  private final DeviceAdminUseCases deviceAdminUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public PosAdminDeviceController(
      DeviceAdminUseCases deviceAdminUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.deviceAdminUseCases = deviceAdminUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosDeviceList
  public PagedResponse<PosDeviceAdminResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @RequestParam(value = "headquarterId", required = false) Long headquarterId,
      @ModelAttribute PageableRequest pageable) {
    Long hq = headquarterAccessService.enforceHeadquarterFilter(principal, headquarterId);
    return PagedResponse.map(
        deviceAdminUseCases.list(hq, pageable.toPageable()),
        PosWebMapper::toDeviceAdminResponse);
  }

  @PostMapping("/{id}/revoke")
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosDeviceRevoke
  public PosDeviceAdminResponse revoke(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable("id") UUID id) {
    PosDevice device = deviceAdminUseCases.get(id);
    headquarterAccessService.requireHeadquarterAccess(principal, device.getHeadquarterId());
    return PosWebMapper.toDeviceAdminResponse(deviceAdminUseCases.revoke(id));
  }
}
