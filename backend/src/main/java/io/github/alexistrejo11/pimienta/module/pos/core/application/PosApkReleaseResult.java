package io.github.alexistrejo11.pimienta.module.pos.core.application;

/** Latest Android POS APK download payload for the public API. */
public record PosApkReleaseResult(
    String versionName, int versionCode, String url, long expiresInSeconds) {}
