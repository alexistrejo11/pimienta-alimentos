package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc;

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
@Operation(
    summary = "Delete operating expense",
    description = "Soft delete. Receipt objects stay in S3. ADMIN and DIRECTOR only.")
@ApiResponse(responseCode = "204", description = "Deleted.")
public @interface DocOperatingExpenseDelete {}
