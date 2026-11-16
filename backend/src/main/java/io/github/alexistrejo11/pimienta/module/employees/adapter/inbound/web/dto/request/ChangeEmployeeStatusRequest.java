package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "ChangeEmployeeStatusRequest",
    description = "Direct employee lifecycle status update.")
public record ChangeEmployeeStatusRequest(
    @NotNull
        @Schema(
            description =
                "Target status: ACTIVE, INACTIVE, RESIGNED, TERMINATED, or DOWN.",
            example = "ACTIVE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        EmployeeStatus status) {}
