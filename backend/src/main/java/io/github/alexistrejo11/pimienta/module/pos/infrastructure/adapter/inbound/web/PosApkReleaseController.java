package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosApkReleaseResult;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosApkReleaseUseCases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosApkReleaseLatest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc.DocPosApkReleases;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosApkReleaseResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/pos/releases")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocPosApkReleases
public class PosApkReleaseController {

  private final PosApkReleaseUseCases posApkReleaseUseCases;

  public PosApkReleaseController(PosApkReleaseUseCases posApkReleaseUseCases) {
    this.posApkReleaseUseCases = posApkReleaseUseCases;
  }

  @GetMapping("/android/latest")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocPosApkReleaseLatest
  public PosApkReleaseResponse latestAndroid() {
    PosApkReleaseResult result = posApkReleaseUseCases.getLatestAndroidRelease();
    return new PosApkReleaseResponse(
        result.versionName(),
        result.versionCode(),
        result.url(),
        result.expiresInSeconds(),
        result.uploadedAt());
  }
}
