package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "CreateEnrollmentCodeRequest")
public record CreateEnrollmentCodeRequest(@NotNull Long headquarterId) {}
