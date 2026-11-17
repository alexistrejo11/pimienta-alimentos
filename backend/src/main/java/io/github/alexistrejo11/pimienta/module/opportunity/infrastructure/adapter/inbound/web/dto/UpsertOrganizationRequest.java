package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OrganizationKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "UpsertOrganizationRequest")
public record UpsertOrganizationRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull OrganizationKind kind,
    @Size(max = 2000) String notes,
    @Size(max = 160) String contactName,
    @Size(max = 160) String contactEmail,
    @Size(max = 40) String contactPhone) {}
