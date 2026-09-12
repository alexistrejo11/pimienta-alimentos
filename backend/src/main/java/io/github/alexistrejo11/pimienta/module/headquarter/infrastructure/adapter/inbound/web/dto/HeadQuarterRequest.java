package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP body to create or update a headquarter; only name is required (format validation). */
@Schema(
    name = "HeadQuarterRequest",
    description =
        """
        JSON body for create or update. **address** and **description** are optional (null or empty \
        strings are normalized in the application/domain layers).""")
public record HeadQuarterRequest(
    @NotBlank(message = "Name is required")
        @Size(max = 512)
        @Schema(
            description = "Display name or unique key for the site.",
            example = "North Plant — Monterrey",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 512)
    String name,
    @Size(max = 1024)
        @Schema(
            description = "Street or location (optional).",
            example = "1200 Industrial Ave, Industrial Park",
            maxLength = 1024)
    String address,
    @Size(max = 4096)
        @Schema(
            description = "Extended notes (optional).",
            example = "Raw materials receiving; 06:00–22:00.",
            maxLength = 4096)
    String description) {}
