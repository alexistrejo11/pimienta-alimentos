package io.github.alexistrejo11.pimienta.module.opportunity.core.application.command;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;

public record UpsertOrganizationCommand(
    String name,
    OrganizationKind kind,
    String notes,
    String contactName,
    String contactEmail,
    String contactPhone) {}
