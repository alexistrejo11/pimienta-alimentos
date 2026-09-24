package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Ingest POS sync events",
    description =
        "Ordered batch ingest by deviceSequence. Per-event results may mix ACCEPTED, DUPLICATE, "
            + "REQUIRES_REVIEW, and REJECTED. A reused deviceSequence with a different eventId is "
            + "REJECTED (not HTTP 500). Idempotent retries keep the same eventId. "
            + "SALE_CONFIRMED persists sale snapshot and applies POS inventory for CONTROLLED lines "
            + "unless the headquarter is stockless. "
            + "Requires device JWT (typ=device, scope=pos:sync).")
@ApiResponse(
    responseCode = "200",
    description = "Per-event ingest results.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosSyncEventsResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncEvents {}
