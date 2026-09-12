package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.ClientResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.CreateClientRequest;
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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(summary = "Create client", description = "Registers a new CRM client.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateClientRequest.class)))
@ApiResponse(
    responseCode = "201",
    description = "Created client.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ClientResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Validation failed.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocClientCreate {}
