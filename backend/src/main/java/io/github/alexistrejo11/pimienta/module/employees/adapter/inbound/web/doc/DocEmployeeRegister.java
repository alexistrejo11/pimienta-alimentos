package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeResponse;
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

/** {@code POST /api/v1/employees} (recommended: multipart with JSON part + optional photo) */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Register employee",
    description =
        """
        Recommended: **`multipart/form-data`** parts:

        + **`employee`**: JSON matching **`RegisterEmployeeRequest`** (`Content-Type`:
          **`application/json`** when possible).
        + **`photo`** (optional): JPEG, PNG, or WebP. Stored as `image/webp`.

        Browsers/Swagger may label **`employee`** as **`application/octet-stream`**; the server parses
        the bytes as UTF-8 JSON anyway. Do not send onboarding fields only as URL query strings.

        For JSON-without-photo only, **`application/json`** is also accepted (alternate operation hidden
        in Swagger). Rate limit: **SENSITIVE_OPERATIONS**.""")
@RequestBody(
    required = true,
    description =
        "Must be **multipart/form-data** with parts **employee** (JSON, RegisterEmployeeRequest) "
            + "and optional **photo** (file).",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema =
                @Schema(
                    implementation = EmployeeRegisterMultipartBody.class,
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
    responseCode = "201",
    description = "Created employee.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EmployeeResponse.class)))
public @interface DocEmployeeRegister {}
