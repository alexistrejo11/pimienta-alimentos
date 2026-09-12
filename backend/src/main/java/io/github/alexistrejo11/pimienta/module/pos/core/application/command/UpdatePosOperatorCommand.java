package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;

public record UpdatePosOperatorCommand(
    String displayName, PosRole posRole, String pin, Long userId, Boolean active) {}
