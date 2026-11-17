package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileAssetResponse;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code GET /api/v1/files/resources} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Search module resource files",
    description =
        """
        Paginated search restricted to the **RESOURCE** category. `module` is required — \
        the server enforces it and returns 400 if absent. Optionally narrow by entity type, \
        entity id, filename, content type, or creation date range. \
        **ROLE_ADMIN** or **ROLE_MANAGER**. Rate limit: **READ_HEAVY**.""")
@Parameters({
  @Parameter(
      name = "module",
      in = ParameterIn.QUERY,
      required = true,
      description = "Module slug to query (e.g. `inventory`, `payroll`, `contracts`).",
      schema = @Schema(type = "string", example = "inventory")),
  @Parameter(
      name = "entityType",
      in = ParameterIn.QUERY,
      description = "Filter by entity-type tag.",
      schema = @Schema(type = "string", example = "inventory-item")),
  @Parameter(
      name = "entityId",
      in = ParameterIn.QUERY,
      description = "Filter by entity id.",
      schema = @Schema(type = "integer", format = "int64", example = "42")),
  @Parameter(
      name = "originalNameContains",
      in = ParameterIn.QUERY,
      description = "Original filename contains (case-insensitive).",
      schema = @Schema(type = "string", example = "report")),
  @Parameter(
      name = "contentTypeContains",
      in = ParameterIn.QUERY,
      description = "MIME type contains (case-insensitive).",
      schema = @Schema(type = "string", example = "pdf")),
  @Parameter(
      name = "createdFrom",
      in = ParameterIn.QUERY,
      description = "Created from (inclusive), ISO-8601.",
      schema = @Schema(type = "string", format = "date-time", example = "2026-05-01T00:00:00")),
  @Parameter(
      name = "createdTo",
      in = ParameterIn.QUERY,
      description = "Created to (inclusive), ISO-8601.",
      schema = @Schema(type = "string", format = "date-time", example = "2026-05-31T23:59:59")),
  @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index.", schema = @Schema(type = "integer", defaultValue = "0")),
  @Parameter(name = "size", in = ParameterIn.QUERY, description = "Page size (max 100).", schema = @Schema(type = "integer", defaultValue = "20"))
})
@ApiResponse(
    responseCode = "200",
    description = "Paged module resource files.",
    content =
        @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = FileAssetResponse.class)),
            schema = @Schema(implementation = PagedResponse.class)))
public @interface DocFileResourcesSearch {}
