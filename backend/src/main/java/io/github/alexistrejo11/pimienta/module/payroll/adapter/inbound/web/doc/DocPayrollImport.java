package io.github.alexistrejo11.pimienta.module.payroll.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
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

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "page",
      in = ParameterIn.QUERY,
      description = "Page index (same as export listing).",
      example = "0",
      schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
  @Parameter(
      name = "size",
      in = ParameterIn.QUERY,
      description = "Page size.",
      example = "20",
      schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
  @Parameter(name = "employeeId", in = ParameterIn.QUERY, description = "Restrict import to employee id.", example = "101"),
  @Parameter(name = "periodId", in = ParameterIn.QUERY, description = "Restrict import to period id.", example = "22"),
  @Parameter(name = "from", in = ParameterIn.QUERY, description = "Worked start date (inclusive).", example = "2026-04-01"),
  @Parameter(name = "to", in = ParameterIn.QUERY, description = "Worked end date (inclusive).", example = "2026-04-30"),
  @Parameter(
      name = "dryRun",
      in = ParameterIn.QUERY,
      description = "If true, validate only; do not persist. Any row error rejects the whole file.",
      schema = @Schema(type = "boolean", defaultValue = "false"))
})
@Operation(summary = "Import payroll records", description = "Bulk create/update payroll records from .xlsx. dryRun=true previews without writes; any row error rejects the whole file.")
@RequestBody(
    required = true,
    description = "Multipart: required part **file** (`.xlsx`). Optional query-style fields `page`, `size`, `employeeId`, `periodId`, `from`, `to` scope the import (same as export).",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object", requiredProperties = {"file"}),
            encoding =
                @Encoding(
                    name = "file",
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
@ApiResponse(
    responseCode = "200",
    description = "Import counters and row errors.",
    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SpreadsheetBulkImportResult.class)))
@ApiResponse(
    responseCode = "400",
    description = "Invalid or empty file.",
    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocPayrollImport {}
