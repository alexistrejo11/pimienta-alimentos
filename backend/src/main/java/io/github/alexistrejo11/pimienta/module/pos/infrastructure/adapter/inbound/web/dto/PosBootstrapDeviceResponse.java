package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapDeviceResponse")
public record PosBootstrapDeviceResponse(
    String id,
    String name,
    String visibleCode,
    String status,
    @Schema(description = "Highest deviceSequence already accepted for this device; null if none.")
        Long lastDeviceSequence) {}
