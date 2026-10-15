package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseSummaryResponse;
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
@Operation(
    summary = "Sum operating expenses",
    description =
        "Total amount for the same filters as the list. Restricted roles are forced to today and their sede.")
@ApiResponse(
    responseCode = "200",
    content = @Content(schema = @Schema(implementation = OperatingExpenseSummaryResponse.class)))
public @interface DocOperatingExpenseSummary {}
