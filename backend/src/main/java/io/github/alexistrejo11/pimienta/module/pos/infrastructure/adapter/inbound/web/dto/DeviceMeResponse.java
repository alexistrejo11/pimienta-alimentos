package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "DeviceMeResponse")
public record DeviceMeResponse(
    String deviceId,
    String visibleCode,
    String status,
    String deviceName,
    PosSiteResponse site,
    String minAppVersion,
    Map<String, Integer> eventSchemaVersions) {}
