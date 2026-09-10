package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosApkReleaseResult;

/** Public latest Android POS APK download metadata. */
public interface PosApkReleaseUseCases {

  PosApkReleaseResult getLatestAndroidRelease();
}
