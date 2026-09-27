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

/** {@code PUT /api/v1/pos/sync/products/{itemId}} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Rename a POS product on this device site",
    description =
        "Device JWT only. Updates the global item name and barcode. SKU is never rewritten. "
            + "A blank barcode, or one equal to the SKU, is stored as null and the scanner uses the SKU. "
            + "The product must already belong to the device headquarter. Requires device JWT (typ=device, scope=pos:sync).")
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
public @interface DocPosSyncRenameProduct {}
