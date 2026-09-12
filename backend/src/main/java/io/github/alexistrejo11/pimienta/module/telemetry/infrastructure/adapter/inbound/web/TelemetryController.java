package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.TelemetryAcceptedResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.WebTelemetryEventRequest;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives browser telemetry without coupling the client to Loki. */
@RestController
@RequestMapping(BASE + "/telemetry")
@RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
public class TelemetryController {

  private final TelemetryIngestionService ingestionService;

  public TelemetryController(TelemetryIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @PostMapping("/web/events")
  public TelemetryAcceptedResponse webEvent(@Valid @RequestBody WebTelemetryEventRequest request) {
    ingestionService.ingestWeb(request);
    return new TelemetryAcceptedResponse(1);
  }

}
