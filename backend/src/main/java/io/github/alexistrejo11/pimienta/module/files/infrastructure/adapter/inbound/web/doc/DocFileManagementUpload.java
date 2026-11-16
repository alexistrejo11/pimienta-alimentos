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

/** {@code POST /api/v1/files/management/upload} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Upload file (admin)",
    description =
        """
        Upload any company file to S3 (`pimienta/sources/`). \
        Send `multipart/form-data` with the binary `file` part plus query parameters. \
        `category` is required for all uploads; `module` is additionally required when \
        `category = RESOURCE`. Max file size: **50 MB**. **ROLE_ADMIN** only. \
        Rate limit: **SENSITIVE_OPERATIONS**.""")
@Parameters({
  @Parameter(
      name = "category",
      in = ParameterIn.QUERY,
      required = true,
      description = "Storage category: `TEMPLATE` | `COMPANY` | `EXTRAS` | `RESOURCE`.",
      schema = @Schema(implementation = String.class,
          allowableValues = {"TEMPLATE", "COMPANY", "EXTRAS", "RESOURCE"},
          example = "TEMPLATE")),
  @Parameter(
      name = "module",
      in = ParameterIn.QUERY,
      required = false,
      description = "Module slug — **required when `category = RESOURCE`** (e.g. `inventory`, `payroll`).",
      schema = @Schema(type = "string", example = "inventory")),
  @Parameter(
      name = "entityType",
      in = ParameterIn.QUERY,
      required = false,
      description = "Optional entity-type tag within the module (e.g. `inventory-item`).",
      schema = @Schema(type = "string", example = "inventory-item")),
  @Parameter(
      name = "entityId",
      in = ParameterIn.QUERY,
      required = false,
      description = "Optional entity id the file is linked to.",
      schema = @Schema(type = "integer", format = "int64", example = "42")),
  @Parameter(
      name = "description",
      in = ParameterIn.QUERY,
      required = false,
      description = "Human-readable note about the file.",
      schema = @Schema(type = "string", example = "Q2 invoice template")),
  @Parameter(
      name = "uploadedByUserId",
      in = ParameterIn.QUERY,
      required = false,
      description = "Id of the user performing the upload (for audit purposes).",
      schema = @Schema(type = "integer", format = "int64", example = "1"))
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
    description = "File stored in S3 and catalog entry created.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = FileAssetResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Empty file, file exceeds 50 MB, missing `category`, or missing `module` for RESOURCE.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocFileManagementUpload {}
