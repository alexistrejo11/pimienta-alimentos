package io.github.alexistrejo11.pimienta.module.opportunity.core.application.command;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import java.time.LocalDate;

public record UpdateRequirementCommand(
    String title,
    RequirementCategory category,
    boolean mandatory,
    LocalDate dueOn,
    String notes,
    Integer sortOrder) {}
