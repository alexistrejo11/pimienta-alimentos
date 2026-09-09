package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/admin/devices")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminDevices
public class PosAdminDeviceController {

  private final DeviceAdminUseCases deviceAdminUseCases;

  public PosAdminDeviceController(DeviceAdminUseCases deviceAdminUseCases) {
    this.deviceAdminUseCases = deviceAdminUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosDeviceList
  public PagedResponse<PosDeviceAdminResponse> list(
      @RequestParam(value = "headquarterId", required = false) Long headquarterId,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        deviceAdminUseCases.list(headquarterId, pageable.toPageable()),
        PosWebMapper::toDeviceAdminResponse);
  }

  @PostMapping("/{id}/revoke")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosDeviceRevoke
  public PosDeviceAdminResponse revoke(@PathVariable("id") UUID id) {
    return PosWebMapper.toDeviceAdminResponse(deviceAdminUseCases.revoke(id));
  }
}
