package io.github.alexistrejo11.pimienta.module.pos.core.domain;

/** Parsed POS Android release manifest stored next to the APK in S3. */
public record PosApkManifest(
    String versionName, int versionCode, String s3Key, String uploadedAt) {}
