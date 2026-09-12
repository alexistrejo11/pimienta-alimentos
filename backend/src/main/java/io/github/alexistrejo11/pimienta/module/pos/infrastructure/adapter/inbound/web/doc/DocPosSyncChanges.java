package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncChangesResponse;
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
    summary = "POS sync changes (deltas)",
    description =
        "Incremental master download since the opaque changes cursor from bootstrap. "
            + "Returns upsert/deactivate operations for products, operators, and policies. "
            + "Invalid or cross-site cursor → 409 (re-bootstrap). Requires device JWT.")
@ApiResponse(
    responseCode = "200",
    description = "Delta batch with nextCursor.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosSyncChangesResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "409",
    description = "Cursor invalid or belongs to another headquarter.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncChanges {}
