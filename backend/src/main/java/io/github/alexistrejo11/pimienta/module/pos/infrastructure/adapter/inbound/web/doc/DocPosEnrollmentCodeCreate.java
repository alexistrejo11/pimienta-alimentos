package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.CreateEnrollmentCodeRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.EnrollmentCodeResponse;
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
@Operation(
    summary = "Create enrollment code",
    description = "One-time six-digit numeric code, TTL 15 minutes, bound to a headquarter.")
@RequestBody(
    required = true,
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateEnrollmentCodeRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "Code created.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EnrollmentCodeResponse.class)))
public @interface DocPosEnrollmentCodeCreate {}
