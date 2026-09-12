package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapCursorsResponse")
public record PosBootstrapCursorsResponse(String changes) {}
