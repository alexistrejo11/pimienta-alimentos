package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapProductResponse;
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
    summary = "Create a POS-sellable product for this device site",
    description =
        "Device JWT only. Headquarter comes from the token. Empty barcode becomes an internal SKU. "
            + "Does not require a staff user. Requires device JWT (typ=device, scope=pos:sync).")
@ApiResponse(
    responseCode = "201",
    description = "Created POS catalog product.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosBootstrapProductResponse.class)))
@ApiResponse(
    responseCode = "409",
    description = "Barcode already exists.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncCreateProduct {}
