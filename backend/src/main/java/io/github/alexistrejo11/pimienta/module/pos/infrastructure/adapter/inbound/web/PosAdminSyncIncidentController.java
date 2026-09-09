package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.AcceptPosSyncIncidentCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncIncidentAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminSyncIncidents;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncIncidentAccept;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncIncidentGet;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosSyncIncidentList;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.AcceptPosSyncIncidentRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncIncidentResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/admin/sync-incidents")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminSyncIncidents
public class PosAdminSyncIncidentController {

  private final PosSyncIncidentAdminUseCases incidentAdminUseCases;

  public PosAdminSyncIncidentController(PosSyncIncidentAdminUseCases incidentAdminUseCases) {
    this.incidentAdminUseCases = incidentAdminUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosSyncIncidentList
  public PagedResponse<PosSyncIncidentResponse> list(
      @RequestParam(value = "headquarterId", required = false) Long headquarterId,
      @RequestParam(value = "openOnly", required = false) Boolean openOnly,
      @ModelAttribute PageableRequest pageable) {
    return PagedResponse.map(
        incidentAdminUseCases.list(headquarterId, openOnly, pageable.toPageable()),
        PosWebMapper::toSyncIncidentResponse);
  }

  @GetMapping("/{incidentId}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosSyncIncidentGet
  public PosSyncIncidentResponse get(@PathVariable("incidentId") UUID incidentId) {
    return PosWebMapper.toSyncIncidentResponse(incidentAdminUseCases.get(incidentId));
  }

  @PostMapping("/{incidentId}/accept")
  @PreAuthorize("hasRole('ADMIN')")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosSyncIncidentAccept
  public PosSyncIncidentResponse accept(
      @PathVariable("incidentId") UUID incidentId,
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody AcceptPosSyncIncidentRequest request) {
    return PosWebMapper.toSyncIncidentResponse(
        incidentAdminUseCases.accept(
            new AcceptPosSyncIncidentCommand(
                incidentId, principal.userId(), request.label(), request.note())));
  }
}
