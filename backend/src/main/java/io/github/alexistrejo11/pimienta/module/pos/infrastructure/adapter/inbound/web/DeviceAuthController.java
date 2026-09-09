package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAuthUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.DeviceIssuedTokens;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceAuth;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceEnroll;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceMe;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceRefresh;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceMeResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceRefreshRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceTokenPairResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/devices")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosDeviceAuth
public class DeviceAuthController {

  private final DeviceAuthUseCases deviceAuthUseCases;

  public DeviceAuthController(DeviceAuthUseCases deviceAuthUseCases) {
    this.deviceAuthUseCases = deviceAuthUseCases;
  }

  @PostMapping("/enroll")
  @RateLimit(profile = RateLimitProfile.STRICT)
  @DocPosDeviceEnroll
  public DeviceEnrollResponse enroll(@Valid @RequestBody DeviceEnrollRequest request) {
    return PosWebMapper.toEnrollResponse(
        deviceAuthUseCases.enroll(PosWebMapper.toEnrollCommand(request)));
  }

  @PostMapping("/refresh")
  @RateLimit(profile = RateLimitProfile.AUTH_SESSION)
  @DocPosDeviceRefresh
  public DeviceTokenPairResponse refresh(@Valid @RequestBody DeviceRefreshRequest request) {
    DeviceIssuedTokens tokens = deviceAuthUseCases.refresh(request.refreshToken());
    return PosWebMapper.toTokenPair(tokens);
  }

  @GetMapping("/me")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosDeviceMe
  public DeviceMeResponse me(@AuthenticationPrincipal DeviceAuthenticationContext device) {
    return PosWebMapper.toMeResponse(deviceAuthUseCases.me(device.deviceId()));
  }
}
