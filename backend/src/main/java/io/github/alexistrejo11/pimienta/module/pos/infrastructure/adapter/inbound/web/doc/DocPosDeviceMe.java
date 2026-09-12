package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceMeResponse;
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
    summary = "Current device",
    description = "Returns status and site for the authenticated device JWT (typ=device).")
@ApiResponse(
    responseCode = "200",
    description = "Device identity.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceMeResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosDeviceMe {}
