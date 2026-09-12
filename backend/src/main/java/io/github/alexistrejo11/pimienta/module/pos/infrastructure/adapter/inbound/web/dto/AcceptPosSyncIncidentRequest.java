package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AcceptPosSyncIncidentRequest")
public record AcceptPosSyncIncidentRequest(
    @NotBlank @Size(max = 128) String label, @NotBlank @Size(max = 1024) String note) {}
