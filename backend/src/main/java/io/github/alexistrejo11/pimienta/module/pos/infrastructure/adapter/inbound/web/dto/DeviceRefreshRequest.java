package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "DeviceRefreshRequest")
public record DeviceRefreshRequest(
    @NotBlank @Schema(description = "Opaque device refresh token") String refreshToken) {}
