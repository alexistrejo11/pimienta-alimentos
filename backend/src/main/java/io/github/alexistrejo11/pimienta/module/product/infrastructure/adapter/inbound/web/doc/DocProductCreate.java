package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductCreateRequest;
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

/** {@code POST /api/v1/products} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Create product",
    description =
        "Creates a sale product. The SKU is assigned in Postgres. "
            + "trackStock creates the linked warehouse item.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProductCreateRequest.class)))
@ApiResponse(
    responseCode = "201",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ProductResponse.class)))
public @interface DocProductCreate {}
