package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "DeviceEnrollResponse")
public record DeviceEnrollResponse(
    String deviceId,
    String visibleCode,
    String status,
    PosSiteResponse site,
    String accessToken,
    String refreshToken,
    long accessTokenExpiresInSeconds,
    long refreshTokenExpiresInSeconds,
    long refreshTokenMaxExpiresInSeconds,
    String minAppVersion,
    Map<String, Integer> eventSchemaVersions,
    @Schema(
            description =
                "Highest deviceSequence already accepted for this device; null if none. "
                    + "Tablet must continue from lastDeviceSequence + 1 after re-enrollment.")
        Long lastDeviceSequence) {}
