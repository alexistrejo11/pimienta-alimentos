package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateRequirementStatusRequest")
public record UpdateRequirementStatusRequest(@NotNull RequirementStatus status) {}
