package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "DeviceRenamePosProductRequest")
public record DeviceRenamePosProductRequest(
    @NotBlank @Size(max = 300) String name, @Size(max = 64) String barcode) {}
