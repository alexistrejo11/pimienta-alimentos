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
    summary = "Rename the authenticated device",
    description =
        "Updates deviceName for the device JWT (typ=device, scope=pos:sync). "
            + "Does not rotate tokens, change visibleCode, or move the site. "
            + "Name is trimmed and must be 1–40 characters.")
@ApiResponse(
    responseCode = "200",
    description = "Updated device identity.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DeviceMeResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Blank name or longer than 40 characters.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked, or caller is not a device token.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosDeviceRename {}
