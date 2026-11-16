package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.ChangeEmployeeStatusRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
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

/** {@code PATCH /api/v1/employees/{id}/status} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Change employee status",
    description =
        """
        Sets employment lifecycle status directly (ACTIVE, INACTIVE, RESIGNED, TERMINATED, DOWN). \
        Exit statuses stamp terminationDate when missing; ACTIVE/INACTIVE clear it. Rate limit: \
        **SENSITIVE_OPERATIONS**.""")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ChangeEmployeeStatusRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "Updated employee.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EmployeeResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocEmployeeChangeStatus {}
