package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "AttendanceResponse", description = "Single employee attendance record.")
public record AttendanceResponse(
    @Schema(description = "Primary key.", example = "42") Long id,
    @Schema(description = "Employee id.", example = "7") Long employeeId,
    @Schema(description = "Employee full name for display.", example = "María López")
        String employeeFullName,
    @Schema(description = "Headquarter id.", example = "3") Long headquarterId,
    @Schema(description = "Calendar work date.", example = "2026-05-14") LocalDate workDate,
    @Schema(description = "Check-in timestamp.") LocalDateTime checkInTime,
    @Schema(description = "Check-out timestamp; null while still checked in.") LocalDateTime checkOutTime,
    @Schema(description = "Lifecycle status.") AttendanceStatus status,
    @Schema(
            description =
                "Check-in evidence: **HTTPS presigned URL** when stored value is an S3 object key; "
                    + "legacy rows may return a plain URL. Empty when no photo was uploaded.")
        String checkInEvidencePhotoUrl,
    @Schema(
            description =
                "Check-out evidence: **HTTPS presigned URL** when stored value is an S3 object key; "
                    + "legacy rows may return a plain URL. Empty when no photo was uploaded.")
        String checkOutEvidencePhotoUrl,
    @Schema(description = "Minutes between check-in and check-out (0 if open).", example = "480")
        long minutesWorked) {}
