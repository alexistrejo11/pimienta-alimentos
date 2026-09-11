package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto.RegisterResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocPublicEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code POST /api/v1/auth/register} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocPublicEndpoint
@Operation(
    summary = "Register a new account",
    description =
        """
        Creates a user in **pending approval** (`PENDING_APPROVAL`). No access token is issued until \
        an administrator approves the account. Rate limit: **STRICT**.""")
@ApiResponse(
    responseCode = "201",
    description = "Account created; Spanish message explains pending admin approval.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = RegisterResponse.class)))
public @interface DocAuthRegister {}
