package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Headquarter id.",
    example = "1",
    schema = @Schema(type = "integer", format = "int64"))
@Operation(summary = "List POS catalog", description = "Paged effective POS catalog for the headquarter.")
@ApiResponse(responseCode = "200", description = "Paged catalog items.")
@ApiResponse(
    responseCode = "404",
    description = "Headquarter not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocHeadquarterPosCatalogList {}
