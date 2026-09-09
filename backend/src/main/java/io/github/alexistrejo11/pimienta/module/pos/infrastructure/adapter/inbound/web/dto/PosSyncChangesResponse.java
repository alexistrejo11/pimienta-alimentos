package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PosSyncChangesResponse")
public record PosSyncChangesResponse(
    @Schema(example = "1") int schemaVersion,
    @Schema(example = "cursor-hq-42-v1847") String nextCursor,
    List<PosSyncChangeOperationResponse> operations) {}
