package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RenameDeviceRequest")
public record RenameDeviceRequest(
    @NotBlank
        @Size(max = 40)
        @Schema(example = "Caja 1", maxLength = 40, requiredMode = Schema.RequiredMode.REQUIRED)
        String deviceName) {

  public RenameDeviceRequest {
    if (deviceName != null) {
      deviceName = deviceName.strip();
    }
  }
}
