package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@Schema(name = "CreatePosOperatorRequest")
public record CreatePosOperatorRequest(
    @NotBlank String displayName,
    @NotNull PosRole posRole,
    @NotBlank @Schema(description = "Write-only PIN; never returned") String pin,
    Long userId,
    @NotEmpty Set<Long> headquarterIds) {}
