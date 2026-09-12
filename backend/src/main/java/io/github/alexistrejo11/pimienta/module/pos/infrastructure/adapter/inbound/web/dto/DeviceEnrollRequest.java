package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "DeviceEnrollRequest")
public record DeviceEnrollRequest(
    @NotBlank @Schema(example = "ENROLL-7K9Q-2M4P") String enrollmentCode,
    @NotNull @Schema(example = "8f3c1a2e-9b4d-4e6a-a1c0-2d5e8f7a9b01") UUID devicePublicId,
    @NotBlank @Schema(example = "Caja 1 · Cafetería Norte") String deviceName,
    @Schema(example = "1.0.0") String appVersion) {}
