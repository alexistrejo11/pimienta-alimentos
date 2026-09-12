package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateClientRequest")
public record CreateClientRequest(
    @NotBlank @Schema(description = "Display name.", example = "Hotel Chain MX") String name,
    @Schema(description = "Legal or trade company name (optional).", example = "Hotel Chain MX SA de CV")
        String companyName) {}
