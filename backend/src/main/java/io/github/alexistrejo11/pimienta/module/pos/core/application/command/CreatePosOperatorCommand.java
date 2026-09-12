package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import java.util.Set;

public record CreatePosOperatorCommand(
    String displayName, PosRole posRole, String pin, Long userId, Set<Long> headquarterIds) {}
