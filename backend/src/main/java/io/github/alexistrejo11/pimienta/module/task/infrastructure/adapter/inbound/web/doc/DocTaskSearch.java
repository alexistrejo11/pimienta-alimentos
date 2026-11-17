package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
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

/** {@code GET /api/v1/tasks} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "page",
      in = ParameterIn.QUERY,
      description = "Zero-based page index.",
      example = "0",
      schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
  @Parameter(
      name = "size",
      in = ParameterIn.QUERY,
      description = "Page size (1–100).",
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
    summary = "Search tasks",
    description =
        """
        Filtered, paginated list. Response shape is **PagedResponse** (items + metadata). Rate \
        limit: **READ_HEAVY**.""")
@ApiResponse(
    responseCode = "200",
    description = "Paged task rows.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResponse.class)))
public @interface DocTaskSearch {}
