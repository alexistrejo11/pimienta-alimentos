package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
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
@Operation(summary = "Get operating expense", description = "Includes one-hour presigned URLs for receipts the caller may see.")
@ApiResponse(
    responseCode = "200",
    content = @Content(schema = @Schema(implementation = OperatingExpenseResponse.class)))
@ApiResponse(responseCode = "404", description = "OperatingExpense not found.")
public @interface DocOperatingExpenseGet {}
