package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code PUT /api/v1/headquarters/{id}} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Headquarter id to update.",
    example = "1",
    schema = @Schema(type = "integer", format = "int64"))
@Operation(
    summary = "Update headquarter",
    description =
        """
        Replaces headquarter fields from **HeadQuarterRequest**. Requires **ROLE_ADMIN**. **404** if \
        not found (`HEADQUARTER_NOT_FOUND`).""")
@RequestBody(
    required = true,
    description = "Same JSON schema as create.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadQuarterRequest.class),
            examples = {
              @ExampleObject(
                  name = "update",
                  summary = "Update",
                  value =
                      """
                      {
                        "name": "North Plant — Monterrey (expansion)",
                        "address": "1200 Industrial Ave, Building 4",
                        "description": "Includes new packaging line."
                      }
                      """)
            }))
@ApiResponse(
    responseCode = "200",
    description = "Updated headquarter.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadQuarterResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Validation failed.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "403",
    description = "Authenticated but missing **ROLE_ADMIN**.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocHeadquarterUpdate {}
