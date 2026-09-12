package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceRefreshRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceTokenPairResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocPublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocPublicEndpoint
@Operation(summary = "Refresh device tokens", description = "Rotates the hashed device refresh token.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceRefreshRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "New token pair.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceTokenPairResponse.class)))
@ApiResponse(
    responseCode = "401",
    description = "Invalid refresh token.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosDeviceRefresh {}
