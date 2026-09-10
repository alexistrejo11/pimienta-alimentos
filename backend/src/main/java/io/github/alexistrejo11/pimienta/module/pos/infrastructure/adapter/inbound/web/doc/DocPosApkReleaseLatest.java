package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosApkReleaseResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocPublicEndpoint
@Operation(
    summary = "Latest Android POS APK",
    description =
        "Returns version metadata and a pre-signed download URL for the CI-published APK "
            + "at the fixed S3 release prefix. Public; no JWT required.")
@ApiResponse(
    responseCode = "200",
    description = "Release metadata and download URL.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PosApkReleaseResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "No APK release has been published yet (manifest missing).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPosApkReleaseLatest {}
