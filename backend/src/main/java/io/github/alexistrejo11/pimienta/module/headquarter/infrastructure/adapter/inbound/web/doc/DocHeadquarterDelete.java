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

/** {@code DELETE /api/v1/headquarters/{id}} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Headquarter id to soft-delete.",
    example = "1",
    schema = @Schema(type = "integer", format = "int64"))
@Operation(
    summary = "Soft-delete headquarter",
    description =
        """
        Soft-deletes the site. Requires **ROLE_ADMIN**. **204** on success. Rate limit: \
        **SENSITIVE_OPERATIONS**. **404** if not found.""")
@ApiResponse(responseCode = "204", description = "No content; headquarter soft-deleted.")
@ApiResponse(
    responseCode = "403",
    description = "Authenticated but missing **ROLE_ADMIN**.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found (`HEADQUARTER_NOT_FOUND`).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocHeadquarterDelete {}
