package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
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
@Operation(summary = "Ingest a POS telemetry batch",
    description = "Accepts up to 50 device diagnostics plus an optional health snapshot. Device and site come from the device JWT.")
@ApiResponse(responseCode = "200", description = "Batch accepted.")
public @interface DocPosTelemetryIngest {}
