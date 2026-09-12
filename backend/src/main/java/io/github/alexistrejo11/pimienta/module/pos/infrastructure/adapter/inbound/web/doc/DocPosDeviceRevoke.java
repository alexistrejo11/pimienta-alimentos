package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosDeviceAdminResponse;
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
    summary = "Revoke POS device",
    description = "Immediately revokes the device and invalidates its refresh token.")
@ApiResponse(
    responseCode = "200",
    description = "Revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosDeviceAdminResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosDeviceRevoke {}
