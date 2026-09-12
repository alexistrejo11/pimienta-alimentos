package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapResponse;
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
    summary = "POS bootstrap snapshot",
    description =
        "Flat atomic snapshot for the device headquarter: operators (with pinHash), "
            + "catalog products in centavos, policies, and a changes cursor for incremental sync. "
            + "Requires device JWT (typ=device, scope=pos:sync).")
@ApiResponse(
    responseCode = "200",
    description = "Bootstrap projection.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosBootstrapResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Device revoked.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncBootstrap {}
