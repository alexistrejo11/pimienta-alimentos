package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Headquarter id.",
    example = "1",
    schema = @Schema(type = "integer", format = "int64"))
@Parameter(
    name = "itemId",
    in = ParameterIn.PATH,
    required = true,
    description = "Global inventory item id.",
    example = "10",
    schema = @Schema(type = "integer", format = "int64"))
@Operation(summary = "Upsert POS catalog item", description = "Creates or updates effective POS fields for an item.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadquarterPosCatalogItemRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "Saved catalog item.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadquarterPosCatalogItemResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Validation failed.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Headquarter or item not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocHeadquarterPosCatalogPut {}
