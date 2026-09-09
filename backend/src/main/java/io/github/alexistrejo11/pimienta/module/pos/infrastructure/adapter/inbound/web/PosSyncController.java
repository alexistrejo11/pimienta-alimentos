package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncEventsUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceSync;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncBootstrap;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncChanges;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncEvents;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncChangesResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/sync")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosDeviceSync
public class PosSyncController {

  private final PosSyncBootstrapUseCases bootstrapUseCases;
  private final PosSyncChangesUseCases changesUseCases;
  private final PosSyncEventsUseCases eventsUseCases;

  public PosSyncController(
      PosSyncBootstrapUseCases bootstrapUseCases,
      PosSyncChangesUseCases changesUseCases,
      PosSyncEventsUseCases eventsUseCases) {
    this.bootstrapUseCases = bootstrapUseCases;
    this.changesUseCases = changesUseCases;
    this.eventsUseCases = eventsUseCases;
  }

  @GetMapping("/bootstrap")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosSyncBootstrap
  public PosBootstrapResponse bootstrap(
      @AuthenticationPrincipal DeviceAuthenticationContext device) {
    return PosWebMapper.toBootstrapResponse(bootstrapUseCases.bootstrap(device.deviceId()));
  }

  @GetMapping("/changes")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosSyncChanges
  public PosSyncChangesResponse changes(
      @AuthenticationPrincipal DeviceAuthenticationContext device, @RequestParam String cursor) {
    return PosWebMapper.toChangesResponse(changesUseCases.changes(device.deviceId(), cursor));
  }

  @PostMapping("/events")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosSyncEvents
  public PosSyncEventsResponse ingestEvents(
      @AuthenticationPrincipal DeviceAuthenticationContext device,
      @Valid @RequestBody PosSyncEventsRequest request) {
    return PosWebMapper.toSyncEventsResponse(
        eventsUseCases.ingest(device.deviceId(), PosWebMapper.toIngestEventsCommand(request)));
  }
}
