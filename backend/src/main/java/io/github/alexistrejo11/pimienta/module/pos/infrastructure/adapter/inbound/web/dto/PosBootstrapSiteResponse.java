package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosBootstrapSiteResponse")
public record PosBootstrapSiteResponse(String id, String name, String address, String currency) {}
