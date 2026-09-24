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

/** {@code PUT /api/v1/pos/sync/products/{itemId}/offer} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Update price and stock policy for a POS product",
    description =
        "Device JWT only. Updates sale price and stock policy on the headquarter catalog row. "
            + "Name, category, availability, and negative-stock limit stay as they are. "
            + "Requires device JWT (typ=device, scope=pos:sync).")
@ApiResponse(
    responseCode = "200",
    description = "Updated POS catalog product.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosBootstrapProductResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Product is not in this headquarter catalog.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosSyncUpdateProductOffer {}
