package io.github.alexistrejo11.pimienta.module.pos.integration.support;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosApkManifest;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosApkReleaseStoragePort;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** In-memory POS APK release storage for integration tests (no real S3). */
public class StubPosApkReleaseStoragePort implements PosApkReleaseStoragePort {

  public static final String PRESIGN_BASE = "https://example.test/presigned/";

  private final AtomicReference<PosApkManifest> manifest =
      new AtomicReference<>(
          new PosApkManifest("1.0", 1, "pimienta/releases/pos/android/latest.apk"));

  public void setManifest(PosApkManifest value) {
    manifest.set(value);
  }

  public void clearManifest() {
    manifest.set(null);
  }

  @Override
  public Optional<PosApkManifest> readManifest(String manifestKey) {
    return Optional.ofNullable(manifest.get());
  }

  @Override
  public String presignedDownloadUrl(String s3Key) {
    return PRESIGN_BASE + s3Key;
  }
}
