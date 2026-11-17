package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "RequirementResponse")
public record RequirementResponse(
    long id,
    long opportunityId,
    String title,
    RequirementCategory category,
    boolean mandatory,
    RequirementStatus status,
    LocalDate dueOn,
    String notes,
    int sortOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
