package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductUpdateRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.response.ProductResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
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

/** {@code PUT /api/v1/products/{id}} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Update product",
    description =
        "Updates name, barcode, and whether the product tracks stock. The SKU stays. Turning"
            + " stock off archives the linked inventory item; **409** `ITEM_HAS_STOCK` if it still"
            + " has stock, **409** `PRODUCT_STOCK_REQUIRED` if a headquarter controls it.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProductUpdateRequest.class)))
@ApiResponse(
    responseCode = "200",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProductResponse.class)))
public @interface DocProductUpdate {}
