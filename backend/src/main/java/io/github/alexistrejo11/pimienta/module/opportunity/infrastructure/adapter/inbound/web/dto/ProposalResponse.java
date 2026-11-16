package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.ProposalKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "ProposalResponse")
public record ProposalResponse(
    long id,
    long opportunityId,
    ProposalKind kind,
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
