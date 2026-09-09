package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapOperatorResponse")
public record PosBootstrapOperatorResponse(
    String id, String displayName, String role, String pinHash, boolean active) {}
