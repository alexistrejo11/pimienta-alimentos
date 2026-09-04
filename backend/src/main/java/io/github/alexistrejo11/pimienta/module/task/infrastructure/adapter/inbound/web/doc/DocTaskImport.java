package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskBulkImportResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
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

/** {@code POST /api/v1/tasks/import} (multipart form) */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Import tasks from spreadsheet",
    description =
        """
        Upload `multipart/form-data` with field **file** (`.xlsx`); bulk upsert by ID column. Query \
        **dryRun=true** validates without writing. Any row error rejects the whole file. Empty \
        file yields **400**. Rate limit: **SENSITIVE_OPERATIONS**.""")
@RequestBody(
    required = true,
    description = "Form part **file**: Excel workbook (.xlsx).",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema =
                @Schema(
                    type = "object",
                    description = "Must include part `file` with the Excel binary.",
                    requiredProperties = {"file"}),
            encoding =
                @Encoding(
                    name = "file",
                    contentType =
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
@ApiResponse(
    responseCode = "200",
    description = "Created/updated/skipped counts and row errors.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = TaskBulkImportResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Empty file or invalid request.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocTaskImport {}
