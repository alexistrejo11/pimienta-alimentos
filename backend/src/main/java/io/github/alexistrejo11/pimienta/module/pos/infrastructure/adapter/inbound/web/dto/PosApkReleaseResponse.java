package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Latest Android POS APK download metadata with a pre-signed URL.")
public record PosApkReleaseResponse(
    @Schema(description = "Android versionName from the published manifest.", example = "1.0")
        String versionName,
    @Schema(description = "Android versionCode from the published manifest.", example = "1")
        int versionCode,
    @Schema(description = "Pre-signed S3 GET URL for the APK.") String url,
    @Schema(description = "URL lifetime in seconds.", example = "86400") long expiresInSeconds,
    @Schema(
            description = "UTC publish timestamp from the manifest (ISO-8601), when available.",
            example = "2026-03-21T15:30:00Z")
        String uploadedAt) {}
