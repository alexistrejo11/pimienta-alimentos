package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code POST /api/v1/tasks} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Create task",
    description = "Creates a task from **TaskRequest** (Jakarta format validation on the body).")
@RequestBody(
    required = true,
    description = "JSON body; **title** is required. **priority** defaults to MEDIUM when omitted.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = TaskRequest.class),
            examples = {
              @ExampleObject(
                  name = "minimal",
                  summary = "Title only",
                  value =
                      """
                      {
                        "title": "Follow up with supplier"
                      }
                      """),
              @ExampleObject(
                  name = "full",
                  summary = "With checklist and scopes",
                  value =
                      """
                      {
                        "title": "Prepare samples",
                        "description": "Ship before Friday.",
                        "priority": "HIGH",
                        "dueDate": "2026-04-20T18:00:00",
                        "headquarterId": 1,
                        "createdById": 3,
                        "checklist": [
                          { "description": "Order packaging", "displayOrder": 0 },
                          { "description": "Label boxes", "displayOrder": 1 }
                        ]
                      }
                      """)
            }))
@ApiResponse(
    responseCode = "201",
    description = "Created task.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = TaskResponse.class)))
@ApiResponse(
    responseCode = "400",
    description = "Validation failed (e.g. blank title) or other bad request.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocTaskCreate {}
