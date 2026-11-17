package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(name = "CreateRequirementRequest")
public record CreateRequirementRequest(
    @NotBlank @Size(max = 300) String title,
    @NotNull RequirementCategory category,
    @NotNull Boolean mandatory,
    LocalDate dueOn,
    @Size(max = 2000) String notes,
    Integer sortOrder) {}
