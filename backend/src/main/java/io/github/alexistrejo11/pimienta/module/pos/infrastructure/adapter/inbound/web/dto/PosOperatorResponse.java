package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(name = "PosOperatorResponse")
public record PosOperatorResponse(
    Long id,
    String displayName,
    PosRole posRole,
    Long userId,
    boolean active,
    Set<Long> headquarterIds) {}
