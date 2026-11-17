package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "OpportunityResponse")
public record OpportunityResponse(
    long id,
    long organizationId,
    String title,
    OpportunityStatus status,
    String sourceUrl,
    String externalReference,
    LocalDate publishedOn,
    LocalDate submissionDeadline,
    LocalDate decisionOn,
    String notes,
    Long contractId,
    LocalDateTime submittedAt,
    LocalDateTime closedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
