package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "ClientResponse")
public record ClientResponse(
    @Schema(example = "1") Long id,
    @Schema(example = "Hotel Chain MX") String name,
    @Schema(example = "Hotel Chain MX SA de CV") String companyName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
