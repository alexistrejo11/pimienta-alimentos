package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AssignOperatorHeadquarterRequest")
public record AssignOperatorHeadquarterRequest(@NotNull Long headquarterId) {}
