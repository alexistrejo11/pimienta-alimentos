package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterPosSettingsUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosSettings;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosSettingsGet;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterPosSettingsPut;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSettingsRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSettingsResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/headquarters/{id}/pos-settings")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocHeadquarterPosSettings
public class HeadquarterPosSettingsController {

  private final HeadquarterPosSettingsUseCases posSettingsUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public HeadquarterPosSettingsController(
      HeadquarterPosSettingsUseCases posSettingsUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.posSettingsUseCases = posSettingsUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosSettingsGet
  public PosSettingsResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    PosOperationalConfig config = posSettingsUseCases.get(headquarterId);
    return HeadquarterPosWebMapper.toResponse(config);
  }

  @PutMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosSettingsPut
  public PosSettingsResponse put(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable("id") Long headquarterId,
      @Valid @RequestBody PosSettingsRequest request) {
    headquarterAccessService.requireHeadquarterAccess(principal, headquarterId);
    PosOperationalConfig saved =
        posSettingsUseCases.upsert(headquarterId, HeadquarterPosWebMapper.toCommand(request));
    return HeadquarterPosWebMapper.toResponse(saved);
  }
}
