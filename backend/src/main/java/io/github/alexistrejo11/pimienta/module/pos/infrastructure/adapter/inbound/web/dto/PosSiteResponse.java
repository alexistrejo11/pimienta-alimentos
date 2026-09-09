package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosSiteResponse")
public record PosSiteResponse(String id, String name, String address, String currency) {}
