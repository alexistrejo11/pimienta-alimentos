package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncIncidentResponse;
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
    summary = "Accept POS sync incident",
    description =
        "ADMIN only. Classification with required label and note. Does not mutate the sale ticket.")
@ApiResponse(
    responseCode = "200",
    description = "Accepted.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosSyncIncidentResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Authenticated but not ADMIN.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "409",
    description = "Already accepted.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncIncidentAccept {}
