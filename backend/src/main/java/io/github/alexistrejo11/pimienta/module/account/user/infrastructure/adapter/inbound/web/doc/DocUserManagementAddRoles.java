package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto.UserResponse;
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

/** {@code POST /api/v1/users/management/{id}/roles} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Replace user roles",
    description =
        """
        Adds roles from the request body (enum names such as `MANAGER`, `ADMIN`). Idempotent merge \
        semantics are defined by the domain. Rate limit: **SENSITIVE_OPERATIONS**. Typically **ADMIN**.""")
@ApiResponse(
    responseCode = "200",
    description = "User with updated role set.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UserResponse.class)))
public @interface DocUserManagementAddRoles {}
