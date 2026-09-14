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
@Operation(summary = "Read POS telemetry", description = "HQ-scoped POS health observability endpoint.")
@ApiResponse(responseCode = "200", description = "Telemetry response.")
public @interface DocPosTelemetryEndpoint {}
