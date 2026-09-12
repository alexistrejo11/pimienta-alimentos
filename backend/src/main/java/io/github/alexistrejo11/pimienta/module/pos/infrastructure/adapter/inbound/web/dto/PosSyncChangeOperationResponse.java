package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PosSyncChangeOperationResponse")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PosSyncChangeOperationResponse(
    @Schema(allowableValues = {"upsert", "deactivate"}, example = "upsert") String op,
    @Schema(allowableValues = {"product", "operator", "policies"}, example = "product")
        String entity,
    @Schema(example = "1002") String id,
    Object data) {}
