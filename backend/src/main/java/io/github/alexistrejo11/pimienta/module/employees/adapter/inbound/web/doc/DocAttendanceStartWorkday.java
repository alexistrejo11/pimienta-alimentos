package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.AttendanceResponse;
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

/** {@code POST .../attendance/start-workday} (recommended: multipart with JSON part + optional photo). */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Start workday (check-in)",
    description =
        """
        Recommended: **`multipart/form-data`** parts:

        + **`attendance`**: JSON matching **`StartWorkdayRequest`** (`Content-Type`:
          **`application/json`** when possible).
        + **`checkInEvidencePhoto`** (optional): JPEG, PNG, or WebP. The server stores \
        the object in S3 and persists the **object key**; API responses return a **time-limited HTTPS URL**.

        Browsers/Swagger may label **`attendance`** as **`application/octet-stream`**; the server parses \
        the bytes as UTF-8 JSON anyway.

        For JSON **without** evidence photo only, **`application/json`** is also accepted (alternate \
        operation hidden in Swagger). **409** if an open attendance already exists for the resolved work \
        date (`ATTENDANCE_ALREADY_OPEN`). **404** for unknown employee or headquarter.""")
@RequestBody(
    required = true,
    description =
        "Must be **multipart/form-data** with part **attendance** (JSON, StartWorkdayRequest) and optional "
            + "**checkInEvidencePhoto** (file).",
    content =
        @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema =
                @Schema(
                    implementation = AttendanceStartMultipartBody.class,
                    requiredProperties = {"attendance"},
                    description =
                        "Two form parts: **attendance** (object) and optional **checkInEvidencePhoto** (binary)."),
            encoding = {
              @Encoding(
                  name = "attendance",
                  contentType = MediaType.APPLICATION_JSON_VALUE),
              @Encoding(name = "checkInEvidencePhoto")
            }))
@ApiResponse(
    responseCode = "201",
    description = "Created.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = AttendanceResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Not found.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(
    responseCode = "409",
    description = "Conflict.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocAttendanceStartWorkday {}
