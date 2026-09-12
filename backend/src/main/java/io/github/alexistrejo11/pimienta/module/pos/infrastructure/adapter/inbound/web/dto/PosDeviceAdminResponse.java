package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "PosDeviceAdminResponse")
public record PosDeviceAdminResponse(
    UUID id,
    Long headquarterId,
    String visibleCode,
    String deviceName,
    String appVersion,
    PosDeviceStatus status,
    String minAppVersion) {}
