package io.github.alexistrejo11.pimienta.module.account.user.core.application.command;

import java.util.List;

public record AssignHeadquartersCommand(List<Long> headquarterIds) {}
