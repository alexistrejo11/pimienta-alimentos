package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.AcceptPosSyncIncidentCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.EnrollmentCodeUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosAdminEnrollmentCodes;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosEnrollmentCodeCreate;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.CreateEnrollmentCodeRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.EnrollmentCodeResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper.PosWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/admin/enrollment-codes")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosAdminEnrollmentCodes
public class PosAdminEnrollmentCodeController {

  private final EnrollmentCodeUseCases enrollmentCodeUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public PosAdminEnrollmentCodeController(
      EnrollmentCodeUseCases enrollmentCodeUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.enrollmentCodeUseCases = enrollmentCodeUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocPosEnrollmentCodeCreate
  public EnrollmentCodeResponse create(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestBody CreateEnrollmentCodeRequest request) {
    headquarterAccessService.requireHeadquarterAccess(principal, request.headquarterId());
    return PosWebMapper.toEnrollmentResponse(
        enrollmentCodeUseCases.create(PosWebMapper.toEnrollmentCommand(request)));
  }
}
