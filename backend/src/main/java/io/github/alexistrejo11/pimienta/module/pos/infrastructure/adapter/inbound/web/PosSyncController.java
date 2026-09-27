package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.DeviceCreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosDeviceCatalogUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncEventsUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosDeviceSync;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncBootstrap;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncChanges;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncCreateProduct;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncEvents;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncRenameProduct;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncUpdateProductOffer;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceCreatePosProductRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceRenamePosProductRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceUpdatePosProductOfferRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapProductResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncChangesResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/pos/sync")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosDeviceSync
public class PosSyncController {

  private final PosSyncBootstrapUseCases bootstrapUseCases;
  private final PosSyncChangesUseCases changesUseCases;
  private final PosSyncEventsUseCases eventsUseCases;
  private final PosDeviceCatalogUseCases deviceCatalogUseCases;

  public PosSyncController(
      PosSyncBootstrapUseCases bootstrapUseCases,
      PosSyncChangesUseCases changesUseCases,
      PosSyncEventsUseCases eventsUseCases,
      PosDeviceCatalogUseCases deviceCatalogUseCases) {
    this.bootstrapUseCases = bootstrapUseCases;
    this.changesUseCases = changesUseCases;
    this.eventsUseCases = eventsUseCases;
    this.deviceCatalogUseCases = deviceCatalogUseCases;
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

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosSyncCreateProduct
  public PosBootstrapProductResponse createProduct(
      @AuthenticationPrincipal DeviceAuthenticationContext device,
      @Valid @RequestBody DeviceCreatePosProductRequest request) {
    return PosWebMapper.toProductResponse(
        deviceCatalogUseCases.createProduct(
            device.deviceId(),
            new DeviceCreatePosProductCommand(
                request.name(),
                request.salePriceCentavos(),
                request.saleCategory(),
                request.barcode(),
                request.createdByOperatorId(),
                request.stockPolicy())));
  }

  @PutMapping("/products/{itemId}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosSyncRenameProduct
  public PosBootstrapProductResponse renameProduct(
      @AuthenticationPrincipal DeviceAuthenticationContext device,
      @PathVariable("itemId") long itemId,
      @Valid @RequestBody DeviceRenamePosProductRequest request) {
    return PosWebMapper.toProductResponse(
        deviceCatalogUseCases.renameProduct(device.deviceId(), itemId, request.name(), request.barcode()));
  }

  @PutMapping("/products/{itemId}/offer")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosSyncUpdateProductOffer
  public PosBootstrapProductResponse updateProductOffer(
      @AuthenticationPrincipal DeviceAuthenticationContext device,
      @PathVariable("itemId") long itemId,
      @Valid @RequestBody DeviceUpdatePosProductOfferRequest request) {
    return PosWebMapper.toProductResponse(
        deviceCatalogUseCases.updateProductOffer(
            device.deviceId(), itemId, request.salePriceCentavos(), request.stockPolicy()));
  }
}
