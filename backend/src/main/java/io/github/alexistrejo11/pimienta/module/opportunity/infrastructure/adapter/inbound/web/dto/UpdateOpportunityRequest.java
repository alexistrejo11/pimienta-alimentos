package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "UpdateOpportunityRequest")
public record UpdateOpportunityRequest(
    @NotNull Long organizationId,
    @NotBlank @Size(max = 240) String title,
    @Size(max = 500) String sourceUrl,
    @Size(max = 120) String externalReference,
    LocalDate publishedOn,
    LocalDate submissionDeadline,
    LocalDate decisionOn,
    @Size(max = 4000) String notes) {}
