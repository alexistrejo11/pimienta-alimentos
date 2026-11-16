package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileAssetResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/** {@code POST /api/v1/files/resources/upload} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Upload module resource file",
    description =
        """
        Upload a file scoped to a specific module and optional entity (always stored under \
        `pimienta/sources/resources/{module}/{entityId?}/`). `module` is required. \
        Max file size: **50 MB**. **ROLE_ADMIN** or **ROLE_MANAGER**. \
        Rate limit: **SENSITIVE_OPERATIONS**.""")
@Parameters({
  @Parameter(
      name = "module",
      in = ParameterIn.QUERY,
      required = true,
      description = "Module slug that owns this resource file (e.g. `inventory`, `payroll`, `contracts`).",
      schema = @Schema(type = "string", example = "inventory")),
  @Parameter(
      name = "entityType",
      in = ParameterIn.QUERY,
      required = false,
      description = "Optional domain entity-type tag within the module (e.g. `inventory-item`).",
      schema = @Schema(type = "string", example = "inventory-item")),
  @Parameter(
      name = "entityId",
      in = ParameterIn.QUERY,
      required = false,
      description = "Optional entity id; creates a sub-folder in S3 for easy grouping.",
      schema = @Schema(type = "integer", format = "int64", example = "42")),
  @Parameter(
      name = "description",
      in = ParameterIn.QUERY,
      required = false,
      description = "Human-readable note about the file.",
      schema = @Schema(type = "string", example = "Stock report for warehouse A")),
  @Parameter(
      name = "uploadedByUserId",
      in = ParameterIn.QUERY,
      required = false,
      description = "Id of the user performing the upload (audit).",
      schema = @Schema(type = "integer", format = "int64", example = "5"))
})
@RequestBody(
    required = true,
    description = "Multipart body — the `file` part carries the raw binary.",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema =
                @Schema(
                    type = "object",
                    description = "Must include the `file` part. All other params are query params.",
                    requiredProperties = {"file"}),
            encoding = @Encoding(name = "file", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)))
@ApiResponse(
    responseCode = "201",
    description = "Resource file stored in S3 and catalog entry created.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = FileAssetResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Empty file, file exceeds 50 MB, or missing `module`.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocFileResourcesUpload {}
