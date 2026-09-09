package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/headquarters/{id}/pos-settings")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocHeadquarterPosSettings
public class HeadquarterPosSettingsController {

  private final HeadquarterPosSettingsUseCases posSettingsUseCases;

  public HeadquarterPosSettingsController(HeadquarterPosSettingsUseCases posSettingsUseCases) {
    this.posSettingsUseCases = posSettingsUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterPosSettingsGet
  public PosSettingsResponse get(@PathVariable("id") Long headquarterId) {
    PosOperationalConfig config = posSettingsUseCases.get(headquarterId);
    return HeadquarterPosWebMapper.toResponse(config);
  }

  @PutMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterPosSettingsPut
  public PosSettingsResponse put(
      @PathVariable("id") Long headquarterId, @Valid @RequestBody PosSettingsRequest request) {
    PosOperationalConfig saved =
        posSettingsUseCases.upsert(headquarterId, HeadquarterPosWebMapper.toCommand(request));
    return HeadquarterPosWebMapper.toResponse(saved);
  }
}
