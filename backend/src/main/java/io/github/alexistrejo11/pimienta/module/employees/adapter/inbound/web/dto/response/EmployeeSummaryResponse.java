package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response;

import java.util.List;

public record EmployeeSummaryResponse(
    long totalNotDeleted, List<DepartmentHeadcountResponse> headcountByDepartment) {}
