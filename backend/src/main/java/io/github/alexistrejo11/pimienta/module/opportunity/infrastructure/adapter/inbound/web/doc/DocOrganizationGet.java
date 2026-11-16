package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.inbound.web.dto.OrganizationResponse;
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
@Operation(summary = "Get organization by id")
@ApiResponse(
    responseCode = "200",
    content = @Content(schema = @Schema(implementation = OrganizationResponse.class)))
public @interface DocOrganizationGet {}
