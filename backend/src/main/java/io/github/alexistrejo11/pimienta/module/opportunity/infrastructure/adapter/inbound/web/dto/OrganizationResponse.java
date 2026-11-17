package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "OrganizationResponse")
public record OrganizationResponse(
    long id,
    String name,
    OrganizationKind kind,
    String notes,
    String contactName,
    String contactEmail,
    String contactPhone,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
