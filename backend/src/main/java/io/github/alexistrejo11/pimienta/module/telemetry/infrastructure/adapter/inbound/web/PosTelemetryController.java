package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryBatchRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.TelemetryAcceptedResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.doc.DocPosTelemetryDevice;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.doc.DocPosTelemetryIngest;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives durable POS telemetry using the existing device authorization boundary. */
@RestController
@RequestMapping(BASE + "/pos/telemetry")
@RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
@DocPosTelemetryDevice
public class PosTelemetryController {

  private final TelemetryIngestionService ingestionService;

  public PosTelemetryController(TelemetryIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping("/events")
  @DocPosTelemetryIngest
  public TelemetryAcceptedResponse events(
      @AuthenticationPrincipal DeviceAuthenticationContext device,
      @Valid @RequestBody PosTelemetryBatchRequest request) {
    return new TelemetryAcceptedResponse(ingestionService.ingestPos(device, request));
  }
}
