package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosApkManifest;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosApkNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosApkReleaseUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosApkReleaseStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PosApkReleaseUseCasesImpl implements PosApkReleaseUseCases {

  static final long PRESIGN_TTL_SECONDS = 86_400L;

  private final PosApkReleaseStoragePort storagePort;
  private final String defaultApkKey;
  private final String manifestKey;

  public PosApkReleaseUseCasesImpl(
      PosApkReleaseStoragePort storagePort,
      @Value("${aws.s3.pos-apk-key}") String defaultApkKey,
      @Value("${aws.s3.pos-apk-manifest-key}") String manifestKey) {
    this.storagePort = storagePort;
    this.defaultApkKey = defaultApkKey;
    this.manifestKey = manifestKey;
  }

  @Override
  public PosApkReleaseResult getLatestAndroidRelease() {
    PosApkManifest manifest =
        storagePort
            .readManifest(manifestKey)
            .orElseThrow(() -> new PosApkNotFoundException(manifestKey));

    String apkKey =
        StringUtils.hasText(manifest.s3Key()) ? manifest.s3Key().trim() : defaultApkKey;
    String url = storagePort.presignedDownloadUrl(apkKey);

    return new PosApkReleaseResult(
        manifest.versionName(), manifest.versionCode(), url, PRESIGN_TTL_SECONDS);
  }
}
