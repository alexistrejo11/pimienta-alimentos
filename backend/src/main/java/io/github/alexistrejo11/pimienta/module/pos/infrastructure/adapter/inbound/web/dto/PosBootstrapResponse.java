package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(name = "PosBootstrapResponse")
public record PosBootstrapResponse(
    int schemaVersion,
    String kind,
    UUID snapshotId,
    Instant generatedAt,
    PosBootstrapSiteResponse site,
    PosBootstrapDeviceResponse device,
    List<PosBootstrapOperatorResponse> operators,
    List<PosBootstrapProductResponse> products,
    List<String> openAmountCategories,
    PosBootstrapPoliciesResponse policies,
    PosBootstrapCursorsResponse cursors) {}
