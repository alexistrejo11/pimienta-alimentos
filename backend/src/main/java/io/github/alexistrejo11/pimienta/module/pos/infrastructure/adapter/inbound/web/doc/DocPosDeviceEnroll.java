package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollResponse;
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
@Operation(
    summary = "Enroll POS device",
    description = "Consumes a one-time six-digit numeric enrollment code (TTL 15 min) and returns device tokens.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceEnrollRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "Device enrolled.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceEnrollResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Invalid or expired enrollment code.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "409",
    description = "Code already used or device already enrolled.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosDeviceEnroll {}
