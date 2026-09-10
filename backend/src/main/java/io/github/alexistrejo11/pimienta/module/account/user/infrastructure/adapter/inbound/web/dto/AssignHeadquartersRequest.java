package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignHeadquartersRequest(@NotNull List<Long> headquarterIds) {}
