package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code GET /api/v1/tasks/export} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "page",
      in = ParameterIn.QUERY,
      description = "Present for Spring binding; export is not limited to one page.",
      example = "0",
      schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
  @Parameter(
      name = "size",
      in = ParameterIn.QUERY,
      description = "Present for Spring binding; export ignores single-page limits.",
      example = "20",
      schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
  @Parameter(
      name = "headquarterId",
      in = ParameterIn.QUERY,
      description = "Filter by headquarter id.",
      example = "1",
      schema = @Schema(type = "integer", format = "int64")),
  @Parameter(
      name = "employeeId",
      in = ParameterIn.QUERY,
      description = "Filter by assignee (employee) id.",
      example = "7",
      schema = @Schema(type = "integer", format = "int64")),
  @Parameter(
      name = "status",
      in = ParameterIn.QUERY,
      description = "Filter by workflow status.",
      example = "PENDING",
      schema = @Schema(implementation = Task.Status.class))
})
@Operation(
    summary = "Export tasks to Excel",
    description =
        """
        Streams an `.xlsx` report using the same optional filters as search; all matching rows are \
        exported. `Content-Disposition` suggests a filename. Rate limit: **READ_HEAVY**.""")
@ApiResponse(
    responseCode = "200",
    description = "Excel workbook bytes.",
    content =
        @Content(
            mediaType =
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
public @interface DocTaskExport {}
