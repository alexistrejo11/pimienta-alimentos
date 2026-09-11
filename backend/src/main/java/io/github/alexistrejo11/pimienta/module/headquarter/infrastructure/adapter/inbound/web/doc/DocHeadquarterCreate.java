package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
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

/** {@code POST /api/v1/headquarters} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Create headquarter",
    description =
        """
        Registers a new site from **HeadQuarterRequest** (Jakarta format validation on the body). \
        Requires **ROLE_ADMIN**.""")
@RequestBody(
    required = true,
    description = "JSON body; **name** is required. **address** and **description** are optional.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadQuarterRequest.class),
            examples = {
              @ExampleObject(
                  name = "full",
                  summary = "All fields",
                  value =
                      """
                      {
                        "name": "North Plant — Monterrey",
                        "address": "1200 Industrial Ave, Industrial Park",
                        "description": "Raw materials receiving; dock 3 access."
                      }
                      """),
              @ExampleObject(
                  name = "minimal",
                  summary = "Name only",
                  value =
                      """
                      {
                        "name": "Guadalajara DC"
                      }
                      """)
            }))
@ApiResponse(
    responseCode = "201",
    description = "Created headquarter.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = HeadQuarterResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Validation failed (e.g. blank name) or other bad request.",
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
public @interface DocHeadquarterCreate {}
