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
@Operation(
    summary = "Create operating expense",
    description =
        "Multipart: `data` (application/json) and optional repeated `attachments` images. "
            + "At most 5 images, 5 MB each, 6 MB together.")
@ApiResponse(
    responseCode = "201",
    content = @Content(schema = @Schema(implementation = OperatingExpenseResponse.class)))
public @interface DocOperatingExpenseCreate {}
