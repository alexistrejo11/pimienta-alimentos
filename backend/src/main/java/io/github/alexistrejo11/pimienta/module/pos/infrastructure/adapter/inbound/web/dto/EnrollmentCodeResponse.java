package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "EnrollmentCodeResponse")
public record EnrollmentCodeResponse(
    Long id, String code, Long headquarterId, LocalDateTime expiresAt, LocalDateTime createdAt) {}
