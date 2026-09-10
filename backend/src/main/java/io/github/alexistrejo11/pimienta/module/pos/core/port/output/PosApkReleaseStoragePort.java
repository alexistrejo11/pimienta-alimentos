package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosApkManifest;
import java.util.Optional;

/** Reads the POS APK release manifest and produces download URLs from object storage. */
public interface PosApkReleaseStoragePort {

  Optional<PosApkManifest> readManifest(String manifestKey);

  String presignedDownloadUrl(String s3Key);
}
