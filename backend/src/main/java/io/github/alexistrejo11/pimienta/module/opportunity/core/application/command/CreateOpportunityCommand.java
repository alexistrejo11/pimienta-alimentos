package io.github.alexistrejo11.pimienta.module.opportunity.core.application.command;

import java.time.LocalDate;

public record CreateOpportunityCommand(
    long organizationId,
    String title,
    String sourceUrl,
    String externalReference,
    LocalDate publishedOn,
    LocalDate submissionDeadline,
    LocalDate decisionOn,
    String notes) {}
