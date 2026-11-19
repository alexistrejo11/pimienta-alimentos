package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/** {@code PUT /api/v1/employees/{id}} (recommended: multipart with JSON part + optional photo) */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Identificador del empleado.",
    example = "42")
@Operation(
    summary = "Update employee",
    description =
        """
        Recommended: **`multipart/form-data`** parts:

        + **`employee`**: JSON matching **`UpdateEmployeeRequest`** (`Content-Type`:
          **`application/json`** when possible).
        + **`photo`** (optional): JPEG, PNG, or WebP. Stored as `image/webp`.

        Browsers/Swagger may label **`employee`** as **`application/octet-stream`**; the server parses
        the bytes as UTF-8 JSON anyway.

        For JSON-without-photo only, **`application/json`** is also accepted (alternate operation hidden
        in Swagger). **404** if not found (`EMPLOYEE_NOT_FOUND`). Rate limit: **SENSITIVE_OPERATIONS**.""")
@RequestBody(
    required = true,
    description =
        "Must be **multipart/form-data** with parts **employee** (JSON, UpdateEmployeeRequest) "
            + "and optional **photo** (file).",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema =
                @Schema(
                    implementation = EmployeeUpdateMultipartBody.class,
                    requiredProperties = {"employee"},
                    description =
                        "Two form parts: **employee** (object) and optional **photo** (binary)."),
            encoding = {
              @Encoding(
                  name = "employee",
                  contentType = MediaType.APPLICATION_JSON_VALUE),
              @Encoding(name = "photo")
            }))
@ApiResponse(
    responseCode = "200",
    description = "Updated employee.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EmployeeResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocEmployeeUpdate {}
