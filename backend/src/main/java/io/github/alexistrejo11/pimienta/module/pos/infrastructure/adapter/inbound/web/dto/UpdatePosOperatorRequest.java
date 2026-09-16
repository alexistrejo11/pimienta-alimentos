package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "UpdatePosOperatorRequest")
public record UpdatePosOperatorRequest(
    String displayName,
    PosRole posRole,
    @Schema(description = "Write-only PIN; omit to keep current") String pin,
    Long userId,
    Boolean active,
    Set<Long> headquarterIds) {}
