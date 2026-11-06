package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import java.time.LocalDate;

/** Paginated listing projection (lighter than {@link EmployeeResponse}). */
public record EmployeeListItemResponse(
        Long id,
        String fullName,
        String photoUrl,
        String email,
        String department,
        String position,
        EmployeeStatus status,
        LocalDate hireDate) {

    public EmployeeListItemResponse {
        fullName = fullName != null ? fullName.trim() : "";
        photoUrl = photoUrl != null ? photoUrl.trim() : "";
        email = email != null ? email.trim() : "";
        department = department != null ? department.trim() : "";
        position = position != null ? position.trim() : "";
    }
}
